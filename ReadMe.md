# Larv

**Larv** is a dynamically-typed, interpreted scripting language that runs on the JVM. It is written in Java and designed to be expressive and readable, with a clean C-style syntax, first-class functions, object-oriented programming through classes, a rich standard library, and a seamless Java interoperability (FFI) layer that lets you call any Java class from Larv code.

> **Version:** 1.0.2-beta  
> **Author:** Abd Allah Al Habbash ([@Habbashx](https://github.com/Habbashx))  
> **File extension:** `.larv`

---

## Table of Contents

- [Features](#features)
- [Installation & Building](#installation--building)
- [Running a Larv Program](#running-a-larv-program)
- [Language Overview](#language-overview)
    - [Variables](#variables)
    - [Data Types](#data-types)
    - [Operators](#operators)
    - [Control Flow](#control-flow)
    - [Functions](#functions)
    - [Classes & Objects](#classes--objects)
    - [Arrays](#arrays)
    - [Modules](#modules)
    - [Imports](#imports)
    - [Error Handling](#error-handling)
    - [Enums](#enums)
    - [Java Interop (FFI)](#java-interop-ffi)
- [Standard Library](#standard-library)
- [CLI Reference](#cli-reference)
- [License](#license)

---

## Features

- **Dynamic typing** — variables hold any value; no type annotations required
- **First-class functions** — functions are values; can be passed, stored, and returned
- **Object-oriented** — `class` declarations with fields, methods, and an `init` constructor
- **Modules** — group related declarations into named namespaces
- **File imports** — split code across multiple `.larv` files using `import`
- **Error handling** — `try / catch / finally` and `throw`
- **Enums** — named variant sets with `enum`
- **Switch expressions** — multi-branch matching with optional `default`
- **Java FFI** — bind and call any Java class directly with `include ... from`
- **Raw (multi-line) strings** — `"""..."""` triple-quote syntax
- **Rich standard library** — math, strings, I/O, lists, maps, HTTP, regex, date/time, encoding, type conversion, system, and properties
- **Built-in array methods** — `push`, `pop`, `peek`, `slice`, `join`, and more via dot syntax
- **Ternary operator** — `condition ? thenValue, elseValue`
- **Compound assignment** — `+=`, `-=`, `*=`, `/=`
- **`for … in`** — range-based foreach loop
- **`range()`** — Python-style numeric range generation

---

## Installation & Building

Larv is a Maven project. You need **Java 21+** and **Maven** installed.

```bash
git clone https://github.com/Habbashx/larv.git
cd larv
mvn package -q
```

This produces a fat JAR at `target/larv-1.0.0-beta.jar` (exact name depends on the POM configuration). The `MANIFEST.MF` sets the main class, so the JAR is directly executable.

You can create a wrapper script for convenience:

```bash
#!/usr/bin/env bash
# /usr/local/bin/larv
exec java -jar /path/to/larv-1.0.0-beta.jar "$@"
```

```bash
chmod +x /usr/local/bin/larv
```

---

## Running a Larv Program

```bash
# Execute a source file
larv run hello.larv

# Print the interpreter version
larv --version

# Print creator info
larv --creator
```

A minimal `hello.larv`:

```larv
print("Hello, World!")
```

---

## Language Overview

### Variables

```larv
var name = "Larv"       // mutable variable
const PI = 3.14159      // immutable constant (write-once)
var x                   // declared without a value → nil
```

Variables are dynamically typed. `var` is mutable; `const` enforces a single assignment.

---

### Data Types

| Type    | Example                          | Notes                            |
|---------|----------------------------------|----------------------------------|
| Number  | `42`, `3.14`, `-7`               | All numbers are 64-bit doubles   |
| String  | `"hello"`, `"line\none"`         | Escape sequences: `\n \t \r \\` `\"` |
| Raw String | `"""multi\nline"""`           | No escape processing; verbatim   |
| Boolean | `true`, `false`                  |                                  |
| Nil     | `nil`                            | Absence of a value               |
| Array   | `[1, 2, 3]`                      | Heterogeneous, zero-indexed      |
| Object  | `new Point()`                    | Instance of a user-defined class |

---

### Operators

**Arithmetic**

```larv
x + y    x - y    x * y    x / y
x++      x--
x += 5   x -= 2   x *= 3   x /= 4
```

**Comparison**

```larv
x == y   x != y   x < y   x > y   x <= y   x >= y
```

**Logical**

```larv
a && b    a || b    !a
```

**Ternary**

```larv
var label = score >= 50 ? "pass", "fail"
```

---

### Control Flow

**if / else if / else**

```larv
if x > 0 {
    print("positive")
} else if x == 0 {
    print("zero")
} else {
    print("negative")
}
```

**while**

```larv
var i = 0
while i < 10 {
    print(i)
    i++
}
```

**Traditional for**

```larv
for i = 0; i < 5; i++ {
    print(i)
}
```

**for … in (foreach)**

```larv
var fruits = ["apple", "banana", "cherry"]
for fruit in fruits {
    print(fruit)
}

// Works with range() too
for n in range(5) {
    print(n)   // 0 1 2 3 4
}
```

**break / continue**

```larv
for i in range(10) {
    if i == 3 { continue }
    if i == 7 { break }
    print(i)
}
```

**switch**

```larv
switch day {
    case "Mon", "Tue", "Wed", "Thu", "Fri" : {
        print("Weekday")
    }
    case "Sat", "Sun" : {
        print("Weekend")
    }
    default : {
        print("Unknown day")
    }
}
```

Multiple comma-separated values on a single `case` arm act as OR conditions.

---

### Functions

```larv
func greet(name) {
    return "Hello, " + name + "!"
}

print(greet("World"))
```

Functions are first-class values and can be stored in variables or passed as arguments:

```larv
func apply(fn, value) {
    return fn(value)
}

func double(x) { return x * 2 }

print(apply(double, 21))   // 42
```

Recursion is fully supported:

```larv
func factorial(n) {
    if n <= 1 { return 1 }
    return n * factorial(n - 1)
}
```

---

### Classes & Objects

```larv
class Animal {
    func init(name, sound) {
        this.name  = name
        this.sound = sound
    }

    func speak() {
        print(this.name + " says " + this.sound)
    }
}

var dog = new Animal("Dog", "Woof")
dog.speak()             // Dog says Woof
print(dog.name)         // Dog
dog.name = "Buddy"      // Field assignment
```

The `init` method is the constructor. It is called automatically when `new ClassName(args)` is evaluated. Fields are set with `this.field = value`.

---

### Arrays

Arrays are zero-indexed and heterogeneous.

```larv
var arr = [10, "hello", true, nil]

// Index access and assignment
print(arr[0])        // 10
arr[1] = "world"

// Built-in dot methods
arr.push(99)
var last = arr.pop()
var size = len(arr)

arr.reverse()
var sub  = arr.slice(1, 3)
var text = arr.join(", ")
```

Built-in array methods: `push`, `pop`, `peek`, `first`, `last`, `contains`, `indexOf`, `isEmpty`, `clear`, `reverse`, `remove`, `slice`, `join`.

---

### Modules

Modules are named namespaces that group related declarations. Access members with `ModuleName.member`.

```larv
module Geometry {
    const PI = 3.14159

    func circleArea(r) {
        return PI * r * r
    }
}

print(Geometry.circleArea(5))
```

---

### Imports

**Standard library import:**

```larv
import "math"
print(sqrt(16))    // 4.0
```

**File import** (dot-separated relative path, `.larv` extension is implied):

```larv
import "utils.helpers"   // loads ./utils/helpers.larv
```

File imports bring the top-level declarations of the other file into scope.

---

### Error Handling

```larv
try {
    var result = riskyOperation()
} catch (e) {
    printErr("Error: " + e)
} finally {
    print("Cleanup done")
}
```

`throw` can throw any value:

```larv
func divide(a, b) {
    if b == 0 {
        throw "Division by zero"
    }
    return a / b
}

try {
    print(divide(10, 0))
} catch (err) {
    print("Caught: " + err)
}
```

---

### Enums

```larv
enum Direction { NORTH, SOUTH, EAST, WEST }

var heading = Direction.NORTH
print(heading)    // NORTH
```

Enum variants are accessed with `EnumName.VARIANT` dot syntax.

---

### Java Interop (FFI)

Larv can bind and call any Java class on the classpath.

**Static binding** (only static methods):

```larv
include JMath from "java.lang.Math"
print(JMath.sqrt(9))      // 3.0
print(JMath.PI)           // 3.141592653589793
```

**Instance binding with constructor arguments** (`involve`):

```larv
include scanner from "java.util.Scanner" involve {
    "java.lang.System.in"
}
var line = scanner.nextLine()
print("You typed: " + line)
```

Static field references (`"java.lang.System.in"`) and nested constructions are resolved automatically inside the `involve { }` block.

---

## Standard Library

Import a module once per file to activate its functions.

### Core (always available — no import needed)

| Function | Signature | Description |
|---|---|---|
| `print` | `print(value)` | Print to stdout with newline |
| `printErr` | `printErr(value)` | Print to stderr in red |
| `input` | `input()` | Read a line from stdin |
| `len` | `len(array)` | Length of an array |
| `range` | `range(end)` / `range(start, end)` | Generate a numeric list |

---

### `import "math"`

| Function | Description |
|---|---|
| `sqrt(n)` | Square root |
| `pow(base, exp)` | Exponentiation |
| `abs(n)` | Absolute value |
| `floor(n)` | Round down |
| `ceil(n)` | Round up |
| `round(n)` | Round to nearest integer |
| `max(a, b)` | Larger of two values |
| `min(a, b)` | Smaller of two values |
| `log(n)` | Natural logarithm |
| `log10(n)` | Base-10 logarithm |
| `sin(n)` | Sine (radians) |
| `cos(n)` | Cosine (radians) |
| `tan(n)` | Tangent (radians) |
| `asin(n)` | Arc sine |
| `acos(n)` | Arc cosine |
| `atan(n)` | Arc tangent |
| `atan2(y, x)` | Two-argument arc tangent |
| `toRadians(deg)` | Degrees → radians |
| `toDegrees(rad)` | Radians → degrees |
| `random()` | Random float in [0, 1) |
| `randomInt(bound)` | Random integer in [0, bound) |
| `clamp(n, min, max)` | Clamp n between min and max |
| `sign(n)` | -1, 0, or 1 |
| `pi()` | π constant |
| `e()` | e constant |
| `isNaN(n)` | True if NaN |
| `isInfinite(n)` | True if infinite |
| `toInt(n)` | Truncate to integer |

---

### `import "string"`

| Function | Description |
|---|---|
| `strLen(s)` | Character count |
| `strUpper(s)` | Uppercase |
| `strLower(s)` | Lowercase |
| `strTrim(s)` | Strip leading/trailing whitespace |
| `strTrimLeft(s)` | Strip leading whitespace |
| `strTrimRight(s)` | Strip trailing whitespace |
| `strContains(s, sub)` | True if sub is found |
| `strStartsWith(s, prefix)` | True if starts with prefix |
| `strEndsWith(s, suffix)` | True if ends with suffix |
| `strIndexOf(s, sub)` | First index of sub, or -1 |
| `strSlice(s, from, to)` | Substring [from, to) |
| `strReplace(s, old, new)` | Replace first occurrence |
| `strReplaceAll(s, old, new)` | Replace all occurrences |
| `strSplit(s, delim)` | Split into array |
| `strJoin(array, sep)` | Join array with separator |
| `strRepeat(s, n)` | Repeat string n times |
| `strReverse(s)` | Reverse characters |
| `strCharAt(s, i)` | Character at index i |
| `strToNumber(s)` | Parse string to number |
| `strFromNumber(n)` | Convert number to string |
| `strIsEmpty(s)` | True if empty |
| `strPadLeft(s, width, char)` | Left-pad to width |
| `strPadRight(s, width, char)` | Right-pad to width |
| `strChars(s)` | Array of individual characters |

---

### `import "io"`

| Function | Description |
|---|---|
| `readFile(path)` | Read file as string |
| `writeFile(path, content)` | Write string to file (overwrites) |
| `appendFile(path, content)` | Append string to file |
| `readLines(path)` | Read file as array of lines |
| `readBytes(path)` | Read file as byte array |
| `writeBytes(path, bytes)` | Write byte array to file |
| `deleteFile(path)` | Delete a file |
| `fileExists(path)` | True if file exists |
| `isDir(path)` | True if path is a directory |
| `listDir(path)` | Array of filenames in directory |
| `makeDir(path)` | Create directory (and parents) |
| `copyFile(src, dst)` | Copy a file |
| `moveFile(src, dst)` | Move/rename a file |
| `fileSize(path)` | File size in bytes |
| `cwd()` | Current working directory |
| `absPath(path)` | Absolute path |

---

### `import "list"`

| Function | Description |
|---|---|
| `listNew()` | Create an empty list |
| `listAdd(list, value)` | Append value |
| `listAddAt(list, index, value)` | Insert at index |
| `listRemove(list, index)` | Remove element at index |
| `listGet(list, index)` | Get element at index |
| `listSet(list, index, value)` | Set element at index |
| `listSize(list)` | Number of elements |
| `listContains(list, value)` | True if value is present |
| `listIndexOf(list, value)` | First index of value, or -1 |
| `listSlice(list, from, to)` | Sub-list [from, to) |
| `listReverse(list)` | Reverse in place |
| `listSort(list)` | Sort in place |
| `listConcat(a, b)` | Concatenate two lists |
| `listFlat(list)` | Flatten one level of nesting |
| `listUnique(list)` | Remove duplicates |
| `listFill(value, n)` | Create list of n copies of value |
| `listClear(list)` | Remove all elements |
| `listIsEmpty(list)` | True if empty |
| `listFirst(list)` | First element |
| `listLast(list)` | Last element |
| `listPop(list)` | Remove and return last element |
| `listShuffle(list)` | Shuffle in place |

---

### `import "map"`

| Function | Description |
|---|---|
| `mapNew()` | Create an empty map |
| `mapSet(map, key, value)` | Set a key |
| `mapGet(map, key)` | Get a value (nil if missing) |
| `mapHas(map, key)` | True if key exists |
| `mapRemove(map, key)` | Remove a key |
| `mapSize(map)` | Number of entries |
| `mapKeys(map)` | Array of all keys |
| `mapValues(map)` | Array of all values |
| `mapClear(map)` | Remove all entries |
| `mapIsEmpty(map)` | True if empty |
| `mapMerge(a, b)` | Merge b into a (b wins on conflict) |
| `mapContainsValue(map, value)` | True if value is in the map |
| `mapToList(map)` | Array of `[key, value]` pairs |

---

### `import "http"`

Returns a map with keys `status` (number), `body` (string), and `ok` (boolean).

| Function | Description |
|---|---|
| `httpGet(url)` | HTTP GET |
| `httpPost(url, body)` | HTTP POST (plain text) |
| `httpPostJson(url, body)` | HTTP POST (JSON content type) |
| `httpPut(url, body)` | HTTP PUT |
| `httpDelete(url)` | HTTP DELETE |
| `httpRequest(url, method, contentType, body, headers)` | Full custom request |

---

### `import "regex"`

| Function | Description |
|---|---|
| `regexMatch(input, pattern)` | True if entire input matches pattern |
| `regexTest(input, pattern)` | True if pattern found anywhere |
| `regexFind(input, pattern)` | First matching substring |
| `regexFindAll(input, pattern)` | Array of all matches |
| `regexReplace(input, pattern, replacement)` | Replace first match |
| `regexReplaceAll(input, pattern, replacement)` | Replace all matches |
| `regexSplit(input, pattern)` | Split on pattern |
| `regexGroup(input, pattern, group)` | Capture group by index |
| `regexGroups(input, pattern)` | Array of all capture groups |

---

### `import "date"`

| Function | Description |
|---|---|
| `timestamp()` | Unix timestamp in milliseconds |
| `dateNow()` | Current date as `"yyyy-MM-dd"` |
| `timeNow()` | Current time as `"HH:mm:ss"` |
| `dateTimeNow()` | Current datetime as `"yyyy-MM-dd HH:mm:ss"` |
| `dateFormat(ts, pattern)` | Format timestamp with pattern |
| `dateParse(str, pattern)` | Parse date string → timestamp |
| `dateAdd(str, amount, unit)` | Add time unit to date string |
| `dateSub(str, amount, unit)` | Subtract time unit from date string |
| `dateDiff(a, b, unit)` | Difference between two date strings |
| `dayOfWeek(str)` | Day name (`"Monday"`, etc.) |
| `monthName(str)` | Month name (`"January"`, etc.) |
| `year(str)` | Extract year |
| `month(str)` | Extract month |
| `day(str)` | Extract day |
| `hour(str)` | Extract hour |
| `minute(str)` | Extract minute |
| `second(str)` | Extract second |
| `isBefore(a, b)` | True if date a is before b |
| `isAfter(a, b)` | True if date a is after b |

Time units for `dateAdd`/`dateSub`/`dateDiff`: `"seconds"`, `"minutes"`, `"hours"`, `"days"`, `"weeks"`, `"months"`, `"years"`.

---

### `import "encode"` (or `import "base64"`)

| Function | Description |
|---|---|
| `base64Encode(str)` | Base64-encode a string |
| `base64Decode(str)` | Base64-decode a string |
| `base64EncodeUrl(str)` | URL-safe Base64 encode |
| `base64DecodeUrl(str)` | URL-safe Base64 decode |
| `hashMd5(str)` | MD5 hex digest |
| `hashSha1(str)` | SHA-1 hex digest |
| `hashSha256(str)` | SHA-256 hex digest |
| `hashSha512(str)` | SHA-512 hex digest |
| `hexEncode(str)` | Hex-encode a string |
| `hexDecode(hex)` | Hex-decode to string |
| `urlEncode(str)` | Percent-encode for URLs |
| `urlDecode(str)` | Decode a percent-encoded string |

---

### `import "convert"`

| Function | Description |
|---|---|
| `toNumber(value)` | Convert to number |
| `toString(value)` | Convert to string |
| `toBool(value)` | Convert to boolean (`"true"/"yes"/"1"/"on"` → true) |
| `toInt(n)` | Truncate floating-point number to integer |
| `toHex(n)` | Integer → lowercase hex string |
| `toOctal(n)` | Integer → octal string |
| `toBinary(n)` | Integer → binary string |
| `fromHex(str)` | Hex string → number |
| `fromOctal(str)` | Octal string → number |
| `fromBinary(str)` | Binary string → number |
| `toBytes(str)` | String → byte array |
| `fromBytes(bytes)` | Byte array → string |
| `typeOf(value)` | String name of the runtime type |

---

### `import "system"`

| Function | Description |
|---|---|
| `exit(code?)` | Exit the process |
| `getEnv(name)` | Read an environment variable |
| `getArgs()` | Array of command-line arguments |
| `clock()` | Elapsed JVM time in milliseconds |
| `nanoTime()` | Elapsed JVM time in nanoseconds |
| `sleep(ms)` | Pause execution for ms milliseconds |
| `exec(cmd)` | Run a shell command; returns `{exit, out, err, ok}` |
| `osName()` | Operating system name |
| `osArch()` | CPU architecture |
| `freeMemory()` | Free JVM heap in bytes |
| `totalMemory()` | Total JVM heap in bytes |
| `gc()` | Suggest a JVM garbage collection |

---

### `import "properties"`

| Function | Description |
|---|---|
| `loadProp(file)` | Load a `.properties` file |
| `getProp(key)` | Get a property value |
| `setProp(file, key, value)` | Set a property value |
| `saveProp(file)` | Save properties to file |
| `getAllProps()` | Map of all properties |

---

## CLI Reference

```
larv run <file.larv>     Execute a Larv source file
larv --version           Print interpreter version
larv --creator           Print creator information
```

---

## License

[LICENCE](LICENSE.md)