# Larv

> A dynamically-typed, interpreted scripting language that runs on the JVM and speaks Java natively.

Larv is a lightweight, expression-oriented language with a clean, C-style syntax. It runs on the Java Virtual Machine, ships with a rich standard library, and provides a first-class Java interop system — letting you call any Java class from your scripts without writing a single line of Java glue code.

**Version:** 1.0.0-beta  
**Author:** Abd Allah Al Habbash ([@Habbashx](https://github.com/Habbashx))

you can read the docs also
[Docs](Docs.md)

---

### Download the Larv Runtime Environment LRE
[larv-isntaller.exe](runtime/larv-installer.exe)
## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Running a Script](#running-a-script)
- [Quick Start](#quick-start)
- [Language Tour](#language-tour)
- [Standard Library](#standard-library)
- [Java Interop (FFI)](#java-interop-ffi)
- [Built-in Functions](#built-in-functions)
- [Error Messages](#error-messages)
- [Project Structure](#project-structure)

---

## Features

- Clean, readable syntax inspired by modern scripting languages
- Dynamic typing with numbers, strings, booleans, arrays, and `nil`
- First-class functions and closures
- Classes with fields and methods via `this`
- `for`, `while`, and `foreach`-style loops
- Module system: import stdlib libraries or other `.larv` files
- Full Java FFI — bind any Java class with `include`/`from`/`involve`
- Rich standard library: math, strings, lists, maps, I/O, HTTP, system
- Descriptive, line-aware error messages for lexer, parser, and runtime

---

## Requirements

- **Java 21+** (the runtime is a plain JVM application)
- Build: Maven or any Java build tool that compiles the `com.habbashx.larv` package

---

## Running a Script

```
larv run <file.larv>
larv --version
larv --creator
```

Source files conventionally use the `.larv` extension.

---

## Quick Start

```larv
// hello.larv
print "Hello, World!"
```

```larv
// variables and arithmetic
var x = 10
var y = 3.14
var name = "Larv"
print name
print x + y
```

```larv
// functions
func greet(who) {
    return "Hello, " + who + "!"
}
print greet("World")
```

---

## Language Tour

### Comments

```larv
// This is a single-line comment
```

Block comments are not supported; use multiple `//` lines.

---

### Values and Types

| Type    | Examples                    |
|---------|-----------------------------|
| Number  | `42`, `3.14`, `-7`          |
| String  | `"hello"`, `"world\n"`      |
| Boolean | `true`, `false`             |
| Array   | `[1, 2, 3]`, `["a", "b"]`  |
| Nil     | `nil`                       |

Numbers are always 64-bit floating point internally.

**String escapes:** `\"`, `\\`, `\n`, `\t`, `\r`

---

### Variables

```larv
var x = 42          // mutable variable
const PI = 3.14159  // immutable constant
```

Variables declared with `var` can be reassigned. `const` variables cannot.

```larv
var count = 0
count = count + 1   // reassignment
```

---

### Operators

**Arithmetic**

```larv
var a = 10 + 3   // 13
var b = 10 - 3   // 7
var c = 10 * 3   // 30
var d = 10 / 3   // 3.333...
```

**Comparison**

```larv
x == y    // equal
x != y    // not equal
x < y
x > y
x <= y
x >= y
```

**Increment / Decrement** (statement form only)

```larv
i++
i--
```

**String concatenation**

```larv
var msg = "Hello, " + "World!"
```

---

### Printing

```larv
print "Hello"        // using the print keyword
print(42)            // using the built-in function
```

Both forms are equivalent. The `print` keyword does not require parentheses.

---

### Control Flow

**if / else**

```larv
if x > 10 {
    print "big"
} else {
    print "small"
}
```

There is no `else if` keyword; nest another `if` inside the `else` block:

```larv
if x > 10 {
    print "big"
} else {
    if x > 5 {
        print "medium"
    } else {
        print "small"
    }
}
```

**while**

```larv
var i = 0
while i < 5 {
    print i
    i++
}
```

**for**

```larv
for i = 0; i < 5; i++ {
    print i
}
```

The init part supports variable assignment; the increment part supports `++` and `--`.

**foreach** (iterate over arrays)

```larv
var items = [10, 20, 30]
for item : items {
    print item
}
```

**break / continue**

```larv
while true {
    if done {
        break
    }
    continue
}
```

---

### Arrays

```larv
var nums = [1, 2, 3]
print nums[0]         // index access → 1
nums[1] = 99          // index assignment

var matrix = [[1, 2], [3, 4]]
print matrix[0][1]    // nested access → 2
```

**Array methods** (called with dot syntax):

```larv
nums.push(4)          // append
var last = nums.pop() // remove and return last
var top  = nums.peek() // read last without removing
var first = nums.first()
var last  = nums.last()
nums.reverse()         // in-place reverse
var sub = nums.slice(1, 3)  // new array [from, to)
var joined = nums.join(", ") // "1, 99, 3, 4"
nums.clear()
var idx = nums.indexOf(99)
var has = nums.contains(99)
var empty = nums.isEmpty()
nums.remove(0)         // remove by index
```

`len(array)` returns the element count as a global built-in.

---

### Functions

```larv
func add(a, b) {
    return a + b
}

var result = add(3, 4)  // 7
```

Functions are first-class values and can be assigned to variables:

```larv
var double = func multiply(x) {
    return x * 2
}
// Note: call by the declared name
print multiply(5)   // 10
```

Recursion is fully supported:

```larv
func factorial(n) {
    if n <= 1 {
        return 1
    }
    return n * factorial(n - 1)
}
print factorial(5)  // 120
```

---

### Classes

```larv
class Animal {
    func init(name, sound) {
        this.name = name
        this.sound = sound
    }

    func speak() {
        print this.name + " says " + this.sound
    }
}

var dog = new Animal("Rex", "Woof")
dog.speak()          // Rex says Woof
print dog.name       // Rex
dog.name = "Buddy"   // field assignment
```

`new ClassName(args...)` creates an instance. Inside methods, `this` refers to the current instance. There is no inheritance in the current version.

---

### Imports

**Standard library**

```larv
import math
import string
import list
import map
import io
import http
import system
```

**Other Larv files** (relative to the current file's directory, using dot-path notation):

```larv
import "utils.larv"
import "lib/helpers.larv"
```

---

## Standard Library

### `math`

```larv
import math
```

| Function              | Returns  | Description                        |
|-----------------------|----------|------------------------------------|
| `sqrt(n)`             | number   | Square root                        |
| `pow(base, exp)`      | number   | Exponentiation                     |
| `abs(n)`              | number   | Absolute value                     |
| `floor(n)`            | number   | Round down                         |
| `ceil(n)`             | number   | Round up                           |
| `round(n)`            | number   | Round to nearest integer           |
| `max(a, b)`           | number   | Larger of two numbers              |
| `min(a, b)`           | number   | Smaller of two numbers             |
| `log(n)`              | number   | Natural logarithm                  |
| `log10(n)`            | number   | Base-10 logarithm                  |
| `sin(n)`              | number   | Sine (radians)                     |
| `cos(n)`              | number   | Cosine (radians)                   |
| `tan(n)`              | number   | Tangent (radians)                  |
| `asin(n)`             | number   | Arcsine                            |
| `acos(n)`             | number   | Arccosine                          |
| `atan(n)`             | number   | Arctangent                         |
| `atan2(y, x)`         | number   | Two-argument arctangent            |
| `toRadians(n)`        | number   | Degrees → radians                  |
| `toDegrees(n)`        | number   | Radians → degrees                  |
| `random()`            | number   | Random float in [0, 1)             |
| `randomInt(a, b)`     | number   | Random integer in [a, b]           |
| `clamp(n, lo, hi)`    | number   | Clamp n between lo and hi          |
| `sign(n)`             | number   | -1, 0, or 1                        |
| `pi()`                | number   | π (3.14159…)                       |
| `e()`                 | number   | Euler's number (2.71828…)          |
| `isNaN(n)`            | boolean  | True if NaN                        |
| `isInfinite(n)`       | boolean  | True if infinite                   |
| `toInt(n)`            | number   | Truncate to integer                |

---

### `string`

```larv
import string
```

| Function                      | Returns  | Description                          |
|-------------------------------|----------|--------------------------------------|
| `strLen(s)`                   | number   | Character count                      |
| `strUpper(s)`                 | string   | Uppercase                            |
| `strLower(s)`                 | string   | Lowercase                            |
| `strTrim(s)`                  | string   | Strip leading + trailing whitespace  |
| `strTrimLeft(s)`              | string   | Strip leading whitespace             |
| `strTrimRight(s)`             | string   | Strip trailing whitespace            |
| `strContains(s, sub)`         | boolean  | True if s contains sub               |
| `strStartsWith(s, pre)`       | boolean  | True if s starts with pre            |
| `strEndsWith(s, suf)`         | boolean  | True if s ends with suf              |
| `strIndexOf(s, sub)`          | number   | First index of sub, -1 if not found  |
| `strSlice(s, from, to)`       | string   | Substring [from, to)                 |
| `strReplace(s, old, new)`     | string   | Replace first occurrence             |
| `strReplaceAll(s, old, new)`  | string   | Replace all occurrences              |
| `strSplit(s, delim)`          | array    | Split by delimiter                   |
| `strJoin(array, glue)`        | string   | Join array elements with glue        |
| `strRepeat(s, n)`             | string   | Repeat s n times                     |
| `strReverse(s)`               | string   | Reverse characters                   |
| `strCharAt(s, i)`             | string   | Single character at index i          |
| `strToNumber(s)`              | number   | Parse string as number               |
| `strFromNumber(n)`            | string   | Number to string                     |
| `strIsEmpty(s)`               | boolean  | True if empty or whitespace-only     |
| `strPadLeft(s, n, ch)`        | string   | Left-pad to length n with ch         |
| `strPadRight(s, n, ch)`       | string   | Right-pad to length n with ch        |
| `strChars(s)`                 | array    | Array of individual characters       |

---

### `list`

```larv
import list
```

| Function                      | Returns  | Description                         |
|-------------------------------|----------|-------------------------------------|
| `listNew()`                   | array    | Create empty list                   |
| `listAdd(list, val)`          | nil      | Append val                          |
| `listAddAt(list, i, val)`     | nil      | Insert at index i                   |
| `listRemove(list, i)`         | nil      | Remove element at index i           |
| `listGet(list, i)`            | any      | Get element at index i              |
| `listSet(list, i, val)`       | nil      | Set element at index i              |
| `listSize(list)`              | number   | Number of elements                  |
| `listContains(list, val)`     | boolean  | True if list contains val           |
| `listIndexOf(list, val)`      | number   | First index of val, -1 if not found |
| `listSlice(list, from, to)`   | array    | Sub-list [from, to)                 |
| `listReverse(list)`           | array    | New reversed list                   |
| `listSort(list)`              | array    | New sorted list                     |
| `listConcat(a, b)`            | array    | New list = a + b                    |
| `listFlat(list)`              | array    | Flatten one level of nesting        |
| `listUnique(list)`            | array    | Remove duplicates (order preserved) |
| `listFill(val, n)`            | array    | New list of n copies of val         |
| `listClear(list)`             | nil      | Remove all elements                 |
| `listIsEmpty(list)`           | boolean  | True if size == 0                   |
| `listFirst(list)`             | any      | First element                       |
| `listLast(list)`              | any      | Last element                        |
| `listPop(list)`               | any      | Remove and return last element      |
| `listShuffle(list)`           | nil      | Shuffle in place                    |

---

### `map`

```larv
import map
```

Keys must be strings.

| Function                      | Returns  | Description                         |
|-------------------------------|----------|-------------------------------------|
| `mapNew()`                    | map      | Create empty map                    |
| `mapSet(map, key, val)`       | nil      | Put or update a key                 |
| `mapGet(map, key)`            | any      | Get value (nil if missing)          |
| `mapHas(map, key)`            | boolean  | True if key exists                  |
| `mapRemove(map, key)`         | nil      | Remove a key                        |
| `mapSize(map)`                | number   | Number of entries                   |
| `mapKeys(map)`                | array    | All keys                            |
| `mapValues(map)`              | array    | All values                          |
| `mapClear(map)`               | nil      | Remove all entries                  |
| `mapIsEmpty(map)`             | boolean  | True if size == 0                   |
| `mapMerge(a, b)`              | map      | New map = a merged with b (b wins)  |
| `mapContainsValue(map, val)`  | boolean  | True if any value equals val        |
| `mapToList(map)`              | array    | Array of [key, value] pairs         |

---

### `io`

```larv
import io
```

| Function                      | Returns  | Description                          |
|-------------------------------|----------|--------------------------------------|
| `readFile(path)`              | string   | Read entire file as a string         |
| `writeFile(path, content)`    | nil      | Write string to file (overwrite)     |
| `appendFile(path, content)`   | nil      | Append string to file                |
| `readLines(path)`             | array    | Read file as array of lines          |
| `readBytes(path)`             | array    | Read file as array of byte numbers   |
| `writeBytes(path, array)`     | nil      | Write byte-number array to file      |
| `deleteFile(path)`            | boolean  | Delete file, returns true if deleted |
| `fileExists(path)`            | boolean  | True if file exists                  |
| `isDir(path)`                 | boolean  | True if path is a directory          |
| `listDir(path)`               | array    | List entries in a directory          |
| `makeDir(path)`               | nil      | Create directory (and parents)       |
| `copyFile(src, dst)`          | nil      | Copy a file                          |
| `moveFile(src, dst)`          | nil      | Move / rename a file                 |
| `fileSize(path)`              | number   | File size in bytes                   |
| `cwd()`                       | string   | Current working directory            |
| `absPath(path)`               | string   | Resolve to absolute path             |

---

### `http`

```larv
import http
```

All functions return a map with three keys:

| Key      | Type    | Meaning                          |
|----------|---------|----------------------------------|
| `status` | number  | HTTP status code                 |
| `body`   | string  | Response body                    |
| `ok`     | boolean | True if status is 2xx            |

| Function                              | Description                         |
|---------------------------------------|-------------------------------------|
| `httpGet(url)`                        | GET request                         |
| `httpPost(url, body)`                 | POST with plain-text body           |
| `httpPostJson(url, body)`             | POST with `application/json`        |
| `httpPut(url, body)`                  | PUT with plain-text body            |
| `httpDelete(url)`                     | DELETE request                      |
| `httpRequest(method, url, body, type)`| Custom method and content-type      |

Timeout is fixed at 30 seconds.

---

### `system`

```larv
import system
```

| Function             | Returns  | Description                                 |
|----------------------|----------|---------------------------------------------|
| `exit(code)`         | nil      | Exit the process with code                  |
| `getEnv(name)`       | string   | Read environment variable (nil if missing)  |
| `getArgs()`          | array    | Command-line arguments                      |
| `clock()`            | number   | Wall-clock milliseconds since Unix epoch    |
| `nanoTime()`         | number   | Nanoseconds (for benchmarking)              |
| `sleep(ms)`          | nil      | Pause execution for ms milliseconds         |
| `exec(cmd)`          | map      | Run shell command → `{exit, out, err, ok}`  |
| `osName()`           | string   | OS name (e.g. `"Linux"`)                    |
| `osArch()`           | string   | CPU architecture (e.g. `"amd64"`)           |
| `javaVersion()`      | string   | JVM version string                          |
| `freeMemory()`       | number   | JVM free heap bytes                         |
| `totalMemory()`      | number   | JVM total heap bytes                        |
| `gc()`               | nil      | Suggest garbage collection                  |

`exec(cmd)` returns a map with keys `exit` (number), `out` (string), `err` (string), and `ok` (boolean).

---

## Java Interop (FFI)

Larv can bind any Java class on the classpath and call its methods directly.

### Static binding

Use this when you only need to call static methods on a class.

```larv
include Math from "java.lang.Math"
print Math.sqrt(16)    // 4.0
print Math.PI          // field access also works
```

Syntax:

```
include <Alias> from "<fully.qualified.ClassName>"
```

### Instance binding

Use `involve` to pass constructor arguments and hold a live instance.

```larv
include Writer from "java.io.FileWriter" involve { "output.txt" }
Writer.write("hello from Larv!")
Writer.close()
```

```larv
include Scanner from "java.util.Scanner" involve { "java.lang.System.in" }
var line = Scanner.nextLine()
print line
```

Syntax:

```
include <Alias> from "<fully.qualified.ClassName>" involve { "<arg1>", "<arg2>", ... }
```

Inside `involve { ... }`:
- Plain strings and numbers are passed as-is (coerced to match the constructor signature).
- `"java.lang.System.in"` resolves the static field `System.in`.
- Any `"some.Class.field"` resolves a public static field via reflection.
- `"some.Class(arg1, arg2)"` constructs that class with the given literal args.

### Calling methods

After binding, use dot syntax to call any public method:

```larv
include sb from "java.lang.StringBuilder"
sb.append("Hello")
sb.append(", World!")
print sb.toString()
```

---

## Built-in Functions

These are always available without any import:

| Function       | Returns  | Description                              |
|----------------|----------|------------------------------------------|
| `print(value)` | nil      | Print value followed by a newline        |
| `input()`      | string   | Read a line from standard input          |
| `len(array)`   | number   | Number of elements in an array           |

---

## Error Messages

Larv produces structured, human-readable errors with source location:

```
[LEXER ERROR] line 3, col 8: Unexpected character '@'
[PARSER ERROR] line 7, col 1: Expected '{' to open if-body
[RUNTIME ERROR] line 12: pop() called on an empty array
[FFI ERROR] Java class not found: 'com.example.Missing' — check the fully-qualified name
```

Errors exit the process with code 1. Internal interpreter bugs report the Java exception class and ask you to file a bug report.

---

## Project Structure

```
src/main/java/com/habbashx/larv/
├── Main.java                     — CLI entry point
├── error/
│   └── LarvError.java            — Structured error type
├── lexer/
│   ├── Lexer.java                — Tokenizer
│   ├── Token.java                — Token record
│   └── TokenType.java            — All token kinds
├── parser/
│   ├── Parser.java               — Top-level parse driver
│   ├── StatementParser.java      — Statement parsing
│   ├── ExpressionParser.java     — Pratt expression parser
│   ├── ArgumentParser.java       — Parameter/argument parsing
│   ├── Precedence.java           — Operator precedence levels
│   ├── ast/
│   │   ├── statement/            — AST nodes for statements
│   │   └── expression/           — AST nodes for expressions
│   ├── rule/                     — Prefix/infix/statement rules
│   └── stream/TokenStream.java   — Token cursor
└── runtime/
    ├── Interpreter.java          — Top-level executor
    ├── StatementExecutor.java    — Statement dispatch
    ├── ExpressionEvaluator.java  — Expression evaluation
    ├── Environment.java          — Variable scope
    ├── ExecutionContext.java      — Global state + native registry
    ├── BinaryOperator.java       — Arithmetic and string ops
    ├── ArrayMethods.java         — Built-in array dot-methods
    ├── FunctionInvoker.java      — Function call logic
    ├── LoopExecutor.java         — Loop execution
    ├── LarvObject.java           — Class instance representation
    ├── call/
    │   ├── LarvCallable.java     — Callable interface
    │   ├── UserFunction.java     — User-defined functions
    │   └── NativeFunction.java   — Native Java functions
    ├── ffi/
    │   └── JavaClassRegistry.java — Java class binding + invocation
    ├── importer/
    │   └── LarvFileImporter.java  — File import loader
    ├── registry/
    │   └── NativeRegistry.java   — Global built-ins (print, input, len)
    └── stdlib/
        ├── NativeMathLibrary.java
        ├── NativeStringLibrary.java
        ├── NativeListLibrary.java
        ├── NativeMapLibrary.java
        ├── NativeIoLibrary.java
        ├── NativeHttpLibrary.java
        ├── NativeSystemLibrary.java
        └── loader/NativeLibraryLoader.java
```


# MIT LICENSE
[LICENSE](LICENSE.md)