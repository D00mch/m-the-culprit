# Repro: Ktor 3 + Netty `netty-codec-http:4.1.129.Final` Returns 400 for Routes with M and J

This repository reproduces a request parsing issue when using:

- Ktor 3 (also reproducible on every jvm framework or engine).
- Netty `io.netty:netty-codec-http:4.1.129.Final`

## Symptom

Routes that contain some uppercase letters (for example `M`, `J`) can return `400 Bad Request` before Ktor routing is reached.

Example:

- `GET /hello` -> `200 OK`
- `GET /helloM` -> `400 Bad Request`

## Run

```bash
./gradlew run
```

Server listens on `localhost:8080`.

## Reproduce

In another terminal:

```bash
curl -i localhost:8080/hello      # 200
curl -i localhost:8080/helloM     # 400
curl -i localhost:8080/helloAny   # 404
curl -i localhost:8080/hello%4D   # 200
```

Expected:

- `/hello` returns `HTTP/1.1 200 OK`
- `/helloM` returns `HTTP/1.0 400 Bad Request`
- `/helloAny` returns `HTTP/1.0 404 Bad Request`
- `/hello%4D` succeeds (if `/helloM` route exists)

Check character pattern:

```bash
for c in {A..Z}; do
  _path="/hello${c}"
  code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080$_path")
  echo "$code $_path"
done
```

Expected output:

```bash
404 /helloA
404 /helloB
404 /helloC
404 /helloD
404 /helloE
404 /helloF
404 /helloG
404 /helloH
404 /helloI
400 /helloJ
404 /helloK
404 /helloL
400 /helloM
404 /helloN
404 /helloO
404 /helloP
404 /helloQ
404 /helloR
404 /helloS
404 /helloT
404 /helloU
404 /helloV
404 /helloW
404 /helloX
404 /helloY
404 /helloZ
```

`404` means request reached Ktor but route is missing, while `400` indicates parsing failure before routing.

## Why this happens

Netty `4.1.129.Final` request-line token validation can reject valid URI characters because of a bit-mask collision in `HttpUtil.isEncodingSafeStartLineToken`.

## The problem

The problem is in the way the `isEncodingSafeStartLineToken` is implemented.

```java
private static final long ILLEGAL_REQUEST_LINE_TOKEN_OCTET_MASK = 1L << '\n' | 1L << '\r' | 1L << ' ';

public static boolean isEncodingSafeStartLineToken(CharSequence token) {
    int i = 0;
    int lenBytes = token.length();
    int modulo = lenBytes % 4;
    int lenInts = modulo == 0 ? lenBytes : lenBytes - modulo;
    for (; i < lenInts; i += 4) {
        long chars = 1L << token.charAt(i) |
                1L << token.charAt(i + 1) |
                1L << token.charAt(i + 2) |
                1L << token.charAt(i + 3);
        if ((chars & ILLEGAL_REQUEST_LINE_TOKEN_OCTET_MASK) != 0) {
            return false;
        }
    }
    for (; i < lenBytes; i++) {
        long ch = 1L << token.charAt(i);
        if ((ch & ILLEGAL_REQUEST_LINE_TOKEN_OCTET_MASK) != 0) {
            return false;
        }
    }
    return true;
}
```

From [Java specification](https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.19):

> If the promoted type of the left-hand operand is long then only the six lowest-order bits of the right-hand operand are used as the shift distance... The shift distance actually used is therefore always in the range 0 to 63, inclusive.
      
So this `long ch = 1L << token.charAt(i);` is unsave. 'M' is more than 63.

The fix comes in [4.1.130.Final](https://mvnrepository.com/artifact/io.netty/netty-codec-http/4.1.130.Final).