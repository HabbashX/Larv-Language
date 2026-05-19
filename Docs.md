# Larv Language Reference

This document is the complete language reference for **Larv 1.0.0-beta**. It covers every syntactic construct, every built-in, and every standard library function with detailed descriptions and examples.

---

## Table of Contents

1. [Lexical Structure](#1-lexical-structure)
2. [Types and Values](#2-types-and-values)
3. [Variables and Constants](#3-variables-and-constants)
4. [Expressions](#4-expressions)
5. [Statements](#5-statements)
6. [Functions](#6-functions)
7. [Classes and Objects](#7-classes-and-objects)
8. [Arrays](#8-arrays)
9. [Modules](#9-modules)
10. [Imports](#10-imports)
11. [Error Handling](#11-error-handling)
12. [Enums](#12-enums)
13. [Java Interop (FFI)](#13-java-interop-ffi)
14. [Built-in Functions](#14-built-in-functions)
15. [Standard Library](#15-standard-library)
    - [math](#151-math)
    - [string](#152-string)
    - [io](#153-io)
    - [list](#154-list)
    - [map](#155-map)
    - [http](#156-http)
    - [regex](#157-regex)
    - [date](#158-date)
    - [encode](#159-encode)
    - [convert](#1510-convert)
    - [system](#1511-system)
    - [properties](#1512-properties)
16. [Error Types](#16-error-types)
17. [Execution Pipeline](#17-execution-pipeline)

---

## 1. Lexical Structure

### 1.1 Comments

Only single-line comments are supported, introduced by `//`.

```larv
// This entire line is a comment
var x = 10  // This is an inline comment
```

### 1.2 Keywords

The following identifiers are reserved and cannot be used as variable or function names:

```
var      const    if       else     while    for      in
func     return   break    continue class    new      this
include  from     involve  import   module   as       nil
true     false    try      catch    finally  throw    switch
case     default  enum
```

### 1.3 Identifiers

An identifier begins with a Unicode letter or underscore (`_`) and may be followed by any combination of Unicode letters, digits, and underscores. Identifiers are case-sensitive.

```
myVar    _private    camelCase    SCREAMING_SNAKE    x1
```

### 1.4 Numbers

All numeric literals are IEEE 754 64-bit floating-point values at runtime. Both integer and decimal forms are supported.

```
42       0      -7      3.14     100.0
```

### 1.5 Strings

Regular strings are delimited by double quotes `"..."` and support the following escape sequences:

| Sequence | Meaning |
|---|---|
| `\"` | Double quote |
| `\\` | Backslash |
| `\n` | Newline |
| `\t` | Tab |
| `\r` | Carriage return |

### 1.6 Raw Strings

Triple-quoted strings `"""..."""` are verbatim — no escape processing is performed. A newline immediately after the opening `"""` is stripped. They are ideal for multi-line text and embedded queries.

```larv
const query = """
    SELECT *
    FROM users
    WHERE active = 1
"""
```

### 1.7 Operators (by precedence, low to high)

| Precedence | Operators | Associativity |
|---|---|---|
| Lowest | `?` (ternary) | Right |
| Logical OR | `\|\|` | Left |
| Logical AND | `&&` | Left |
| Equality | `==` `!=` | Left |
| Relational | `<` `>` `<=` `>=` | Left |
| Additive | `+` `-` | Left |
| Multiplicative | `*` `/` | Left |
| Unary | `!` `-` | Right |
| Postfix | `++` `--` | — |
| Highest | `.` `[` `(` | Left |

---

## 2. Types and Values

Larv is dynamically typed. Every value has one of the following runtime types:

| Type | Description | `typeOf()` result |
|---|---|---|
| **Number** | 64-bit double | `"Number"` |
| **String** | UTF-16 string | `"String"` |
| **Boolean** | `true` or `false` | `"Boolean"` |
| **Nil** | Absence of value | `"Nil"` |
| **Array** | Ordered mutable list | `"Array"` |
| **Object** | Instance of a user class | `"Object"` |
| **Function** | Callable (user-defined or native) | `"Function"` |

### Truthiness

The following values are **falsy**; everything else is truthy:

- `false`
- `nil`
- The number `0.0`
- The empty string `""`

---

## 3. Variables and Constants

### `var` — Mutable Variable

```larv
var x = 10
var name = "Larv"
var items = [1, 2, 3]
var empty              // nil by default
```

The value of a `var` variable may be reassigned at any time:

```larv
x = 20
name = "updated"
```

### `const` — Immutable Constant

```larv
const MAX = 100
const GREETING = "Hello"
```

`const` requires an initialiser. Reassigning a constant is a runtime error.

### Scope

Variables are lexically scoped. A variable declared inside a block (between `{` and `}`) is only visible within that block and any nested blocks.

---

## 4. Expressions

### 4.1 Literals

```larv
42          // Number
"hello"     // String
true        // Boolean
nil         // Nil
[1, 2, 3]  // Array literal
```

### 4.2 Variable Access

```larv
var answer = 42
print(answer)
```

### 4.3 Arithmetic Expressions

```larv
var sum  = 1 + 2       // 3
var diff = 10 - 4      // 6
var prod = 3 * 7       // 21
var quot = 9 / 4       // 2.25
```

String concatenation also uses `+`:

```larv
var msg = "Hello, " + "World!"
```

### 4.4 Comparison Expressions

```larv
10 == 10     // true
10 != 5      // true
3 < 7        // true
7 > 3        // true
5 <= 5       // true
6 >= 7       // false
```

### 4.5 Logical Expressions

```larv
true && false   // false
true || false   // true
!true           // false
```

### 4.6 Ternary Expression

```
condition ? thenValue, elseValue
```

Note the comma `,` separating the two branches, not a colon.

```larv
var label = x > 0 ? "positive", "non-positive"
```

### 4.7 Assignment Expression

```larv
x = 99
```

### 4.8 Compound Assignment

```larv
x += 5    // x = x + 5
x -= 2    // x = x - 2
x *= 3    // x = x * 3
x /= 4    // x = x / 4
```

### 4.9 Increment / Decrement

Postfix only:

```larv
x++   // x = x + 1
x--   // x = x - 1
```

### 4.10 Array Index Expression

```larv
var arr = [10, 20, 30]
print(arr[0])    // 10
arr[1] = 99      // index assignment
```

### 4.11 Field Access and Method Calls

```larv
obj.fieldName
obj.method(arg1, arg2)
```

### 4.12 Function Calls

```larv
greet("World")
math.sqrt(16)
```

### 4.13 `new` Expression

```larv
var p = new Point(3, 4)
```

Constructs a new instance of the named class and calls `init` if defined.

### 4.14 `this` Expression

Inside a class method, `this` refers to the current instance.

```larv
class Box {
    func init(w, h) {
        this.width  = w
        this.height = h
    }
    func area() {
        return this.width * this.height
    }
}
```

---

## 5. Statements

### 5.1 Variable Declaration

```larv
var x = 10
var y          // nil
const Z = 100
```

### 5.2 Assignment Statement

```larv
x = 42
obj.field = "value"
arr[0] = 99
```

### 5.3 `print` Statement

The dedicated `print` keyword prints directly (shorthand for `print()`):

```larv
print "Hello, World!"
```

The function form `print(value)` is equivalent:

```larv
print("Hello, World!")
```

### 5.4 Expression Statement

Any expression can be used as a statement (typically a function call):

```larv
doSomething()
arr.push(42)
x++
```

### 5.5 `if` / `else`

```larv
if condition {
    // then branch
} else if otherCondition {
    // else-if branch
} else {
    // else branch
}
```

Braces are mandatory. `else if` chains are unlimited.

### 5.6 `while`

```larv
while condition {
    // body
}
```

### 5.7 `for` (Traditional)

```larv
for init; condition; increment {
    // body
}
```

```larv
for i = 0; i < 10; i++ {
    print(i)
}
```

### 5.8 `for … in` (Foreach)

Iterates over every element of an array:

```larv
for element in collection {
    // body
}
```

Works with array literals, variables, and `range()`:

```larv
for x in [1, 2, 3] {
    print(x)
}

for i in range(5) {
    print(i)   // 0 1 2 3 4
}
```

### 5.9 `break` and `continue`

`break` exits the innermost loop immediately. `continue` skips the remainder of the current iteration.

```larv
for i in range(10) {
    if i == 5 { break }
    if i % 2 == 0 { continue }
    print(i)    // 1 3
}
```

### 5.10 `return`

```larv
func add(a, b) {
    return a + b
}
```

`return` without a value returns `nil`. Using `return` outside a function is a runtime error.

### 5.11 `switch`

```larv
switch subject {
    case value1, value2 : {
        // fires if subject == value1 OR subject == value2
    }
    case value3 : {
        // fires if subject == value3
    }
    default : {
        // fires if no case matched
    }
}
```

`subject` is evaluated once. Each `case` compares with `==`. Multiple comma-separated values on one `case` are OR conditions. `default` is optional.

---

## 6. Functions

### 6.1 Declaration

```larv
func name(param1, param2, ...) {
    // body
}
```

Parameters are positional and untyped. Functions have no overloading; later declarations shadow earlier ones.

### 6.2 Return Values

```larv
func square(n) {
    return n * n
}

var result = square(5)    // 25
```

A function without an explicit `return` returns `nil`.

### 6.3 First-Class Functions

Functions are values. They can be stored in variables, passed to other functions, and returned from functions.

```larv
func double(x) { return x * 2 }

var fn = double
print(fn(10))   // 20

func apply(f, x) { return f(x) }
print(apply(double, 7))   // 14
```

### 6.4 Recursion

```larv
func fib(n) {
    if n <= 1 { return n }
    return fib(n - 1) + fib(n - 2)
}

print(fib(10))   // 55
```

### 6.5 Closures

Functions close over the variables in their enclosing scope:

```larv
func makeCounter() {
    var count = 0
    func increment() {
        count++
        return count
    }
    return increment
}

var counter = makeCounter()
print(counter())   // 1
print(counter())   // 2
```

---

## 7. Classes and Objects

### 7.1 Declaration

```larv
class ClassName {
    func init(param1, param2) {
        this.field1 = param1
        this.field2 = param2
    }

    func methodName() {
        return this.field1
    }
}
```

### 7.2 Instantiation

```larv
var obj = new ClassName(arg1, arg2)
```

`new ClassName(args)` creates a new object and immediately calls `init(args)` if it exists.

### 7.3 Field Access and Assignment

```larv
print(obj.field1)
obj.field1 = "new value"
```

Fields do not need to be declared upfront; they are created when first assigned via `this.field`.

### 7.4 Method Calls

```larv
var result = obj.methodName()
```

### 7.5 Example — Stack

```larv
class Stack {
    func init() {
        this.data = []
    }
    func push(value) {
        this.data.push(value)
    }
    func pop() {
        return this.data.pop()
    }
    func peek() {
        return this.data.peek()
    }
    func size() {
        return len(this.data)
    }
    func isEmpty() {
        return len(this.data) == 0
    }
}

var s = new Stack()
s.push(1)
s.push(2)
print(s.pop())    // 2
print(s.size())   // 1
```

---

## 8. Arrays

Arrays are ordered, heterogeneous, mutable sequences indexed from zero.

### 8.1 Literals

```larv
var empty = []
var nums  = [1, 2, 3, 4, 5]
var mixed = [42, "hello", true, nil]
```

### 8.2 Index Access

```larv
var arr = [10, 20, 30]
print(arr[0])    // 10
print(arr[2])    // 30
arr[1] = 99      // mutation
```

Accessing an out-of-bounds index is a runtime error.

### 8.3 Built-in Array Dot Methods

These methods are available on every array value without any import.

| Method | Arguments | Returns | Description |
|---|---|---|---|
| `push(value)` | value | nil | Append to end |
| `pop()` | — | last value | Remove and return last element |
| `peek()` | — | last value | Return last element without removing |
| `first()` | — | first value | Return first element |
| `last()` | — | last value | Return last element |
| `contains(value)` | value | boolean | True if element is present |
| `indexOf(value)` | value | number | First index of value, or -1 |
| `isEmpty()` | — | boolean | True if length is 0 |
| `clear()` | — | nil | Remove all elements |
| `reverse()` | — | nil | Reverse in place |
| `remove(index)` | index | removed value | Remove element at index |
| `slice(from, to)` | from, to | new array | Sub-array from index `from` (inclusive) to `to` (exclusive) |
| `join(sep?)` | separator | string | Join elements with separator (default `""`) |

```larv
var arr = [3, 1, 4, 1, 5]
arr.push(9)
print(arr.pop())            // 9
print(arr.contains(4))      // true
print(arr.indexOf(1))       // 1
arr.reverse()
print(arr.join(", "))       // 5 1 4 1 3
```

### 8.4 `len(array)`

The built-in `len()` function returns the number of elements:

```larv
print(len([1, 2, 3]))   // 3
```

### 8.5 `range(start?, end)`

Generates a numeric list suitable for `for … in` iteration:

```larv
range(5)       // [0, 1, 2, 3, 4]
range(2, 7)    // [2, 3, 4, 5, 6]
```

---

## 9. Modules

Modules are named namespaces that group declarations.

### 9.1 Declaration

```larv
module ModuleName {
    const VALUE = 42

    func helper(x) {
        return x * 2
    }
}
```

### 9.2 Member Access

```larv
print(ModuleName.VALUE)
print(ModuleName.helper(5))
```

### 9.3 Example

```larv
module Config {
    const HOST    = "localhost"
    const PORT    = 8080
    const VERSION = "1.0"

    func baseUrl() {
        return "http://" + HOST + ":" + PORT
    }
}

print(Config.baseUrl())   // http://localhost:8080
```

---

## 10. Imports

### 10.1 Standard Library Import

```larv
import "math"
import "string"
import "io"
```

The library name must be one of the known standard library identifiers. After import, all functions from that library are available in the current scope.

### 10.2 File Import

```larv
import "relative.dotted.path"
```

The dotted path is resolved relative to the project root (the directory containing the entry file). Dots are converted to path separators. The `.larv` extension is appended automatically.

```larv
import "utils.math_helpers"   // loads ./utils/math_helpers.larv
import "models.user"          // loads ./models/user.larv
```

All top-level declarations from the imported file become available in the current scope.

---

## 11. Error Handling

### 11.1 `try / catch / finally`

```larv
try {
    // code that may throw
} catch (variableName) {
    // handle the thrown value; variableName holds it
} finally {
    // always runs, whether or not an error occurred
}
```

`catch` and `finally` are both optional, but at least one must be present.

```larv
try {
    var data = readFile("config.json")
    print(data)
} catch (err) {
    printErr("Failed to read config: " + err)
} finally {
    print("Done")
}
```

### 11.2 `throw`

Any value can be thrown:

```larv
throw "something went wrong"
throw 404
throw new ErrorInfo("Not found")
```

The thrown value is bound to the catch variable:

```larv
try {
    throw "oops"
} catch (e) {
    print(e)   // oops
}
```

### 11.3 Error Propagation

If an exception is not caught in the current function, it propagates up the call stack. If it reaches the top level uncaught, the interpreter prints it in red and exits with code 1.

---

## 12. Enums

### 12.1 Declaration

```larv
enum Status { PENDING, ACTIVE, INACTIVE, DELETED }
```

### 12.2 Access

```larv
var s = Status.ACTIVE
print(s)   // ACTIVE
```

Enum variants are strings at runtime — their names as written.

### 12.3 Using Enums in Switch

```larv
enum Color { RED, GREEN, BLUE }

var c = Color.GREEN

switch c {
    case Color.RED : {
        print("Stop")
    }
    case Color.GREEN : {
        print("Go")
    }
    case Color.BLUE : {
        print("Info")
    }
}
```

---

## 13. Java Interop (FFI)

Larv can bind and call any Java class available on the JVM classpath.

### 13.1 Static Binding

Use `include alias from "fully.qualified.ClassName"` to bind a class under an alias. Static methods on the class become callable on the alias.

```larv
include JMath from "java.lang.Math"

print(JMath.sqrt(25))          // 5.0
print(JMath.max(10, 20))       // 20.0
print(JMath.PI)                // 3.141592653589793
```

### 13.2 Instance Binding (`involve`)

To instantiate a Java class (calling a constructor), add an `involve { }` block with constructor arguments as strings:

```larv
include sb from "java.lang.StringBuilder" involve {
    "Hello"
}
sb.append(" World")
print(sb.toString())   // Hello World
```

### 13.3 Static Field Resolution

Inside `involve { }`, a string of the form `"ClassName.fieldName"` is automatically resolved to the corresponding Java static field:

```larv
include scanner from "java.util.Scanner" involve {
    "java.lang.System.in"
}
var line = scanner.nextLine()
```

### 13.4 Nested Construction

Arguments of the form `"some.Class(arg1, arg2)"` inside `involve { }` construct a nested Java object:

```larv
include pw from "java.io.PrintWriter" involve {
    "java.io.FileWriter(output.txt)"
}
pw.println("Written from Larv")
pw.flush()
```

### 13.5 Type Coercion

Larv automatically coerces values when calling Java methods: numbers become the closest Java numeric type (`int`, `long`, `double`) and strings become `java.lang.String`. Boolean values become `boolean`.

---

## 14. Built-in Functions

These functions are always available without any import.

### `print(value)`

Prints the string representation of `value` to `stdout` followed by a newline. Whole-number doubles are printed without a decimal point (`5.0` prints as `5`).

```larv
print("Hello")    // Hello
print(42)         // 42
print(true)       // true
print(nil)        // nil
print([1, 2])     // [1, 2]
```

### `printErr(value)`

Prints `value` to `stderr` in red ANSI color. Useful for error output inside `catch` blocks.

```larv
try {
    throw "bad state"
} catch (e) {
    printErr("Error: " + e)
}
```

### `input()`

Reads one line from `stdin` and returns it as a string. Returns `nil` on EOF.

```larv
print("Enter your name: ")
var name = input()
print("Hello, " + name)
```

### `len(array)`

Returns the number of elements in an array as a number.

```larv
print(len([1, 2, 3]))   // 3
print(len([]))           // 0
```

### `range(end)` / `range(start, end)`

Returns a `List` of numbers from `start` (inclusive, default `0`) to `end` (exclusive).

```larv
range(5)        // [0, 1, 2, 3, 4]
range(2, 6)     // [2, 3, 4, 5]
range(0, 0)     // []
```

Primarily used with `for … in`:

```larv
for i in range(3) {
    print(i)   // 0  1  2
}
```

---

## 15. Standard Library

### 15.1 `math`

```larv
import "math"
```

#### Mathematical Functions

| Function | Signature | Returns | Description |
|---|---|---|---|
| `sqrt` | `sqrt(n)` | number | Square root of n |
| `pow` | `pow(base, exp)` | number | base raised to exp |
| `abs` | `abs(n)` | number | Absolute value |
| `floor` | `floor(n)` | number | Round toward negative infinity |
| `ceil` | `ceil(n)` | number | Round toward positive infinity |
| `round` | `round(n)` | number | Round to nearest integer |
| `max` | `max(a, b)` | number | Larger of a and b |
| `min` | `min(a, b)` | number | Smaller of a and b |
| `log` | `log(n)` | number | Natural logarithm |
| `log10` | `log10(n)` | number | Base-10 logarithm |
| `sin` | `sin(n)` | number | Sine (n in radians) |
| `cos` | `cos(n)` | number | Cosine (n in radians) |
| `tan` | `tan(n)` | number | Tangent (n in radians) |
| `asin` | `asin(n)` | number | Arc sine → radians |
| `acos` | `acos(n)` | number | Arc cosine → radians |
| `atan` | `atan(n)` | number | Arc tangent → radians |
| `atan2` | `atan2(y, x)` | number | Two-argument arc tangent |
| `toRadians` | `toRadians(deg)` | number | Degrees to radians |
| `toDegrees` | `toDegrees(rad)` | number | Radians to degrees |
| `random` | `random()` | number | Uniform random float in [0, 1) |
| `randomInt` | `randomInt(bound)` | number | Uniform random integer in [0, bound) |
| `clamp` | `clamp(n, min, max)` | number | Clamp n to [min, max] |
| `sign` | `sign(n)` | number | -1, 0, or 1 |
| `pi` | `pi()` | number | π ≈ 3.141592653589793 |
| `e` | `e()` | number | e ≈ 2.718281828459045 |
| `isNaN` | `isNaN(n)` | boolean | True if n is NaN |
| `isInfinite` | `isInfinite(n)` | boolean | True if n is ±∞ |
| `toInt` | `toInt(n)` | number | Truncate to integer (toward zero) |

```larv
import "math"

print(sqrt(144))          // 12.0
print(pow(2, 10))         // 1024.0
print(round(3.7))         // 4.0
print(clamp(15, 0, 10))   // 10.0
print(pi())               // 3.141592653589793
```

---

### 15.2 `string`

```larv
import "string"
```

All functions take a string as the first argument. Indices are zero-based.

| Function | Signature | Returns | Description |
|---|---|---|---|
| `strLen` | `strLen(s)` | number | Number of characters |
| `strUpper` | `strUpper(s)` | string | All characters uppercase |
| `strLower` | `strLower(s)` | string | All characters lowercase |
| `strTrim` | `strTrim(s)` | string | Strip leading and trailing whitespace |
| `strTrimLeft` | `strTrimLeft(s)` | string | Strip leading whitespace |
| `strTrimRight` | `strTrimRight(s)` | string | Strip trailing whitespace |
| `strContains` | `strContains(s, sub)` | boolean | True if sub is a substring |
| `strStartsWith` | `strStartsWith(s, prefix)` | boolean | True if s begins with prefix |
| `strEndsWith` | `strEndsWith(s, suffix)` | boolean | True if s ends with suffix |
| `strIndexOf` | `strIndexOf(s, sub)` | number | First index of sub, or -1 |
| `strSlice` | `strSlice(s, from, to)` | string | Substring from index from (inclusive) to to (exclusive) |
| `strReplace` | `strReplace(s, old, new)` | string | Replace first occurrence of old with new |
| `strReplaceAll` | `strReplaceAll(s, old, new)` | string | Replace all occurrences |
| `strSplit` | `strSplit(s, delim)` | array | Split s on delim |
| `strJoin` | `strJoin(array, sep)` | string | Join array elements with sep |
| `strRepeat` | `strRepeat(s, n)` | string | Concatenate s with itself n times |
| `strReverse` | `strReverse(s)` | string | Reverse the characters |
| `strCharAt` | `strCharAt(s, i)` | string | Single character at index i |
| `strToNumber` | `strToNumber(s)` | number | Parse string as a number |
| `strFromNumber` | `strFromNumber(n)` | string | Convert number to string |
| `strIsEmpty` | `strIsEmpty(s)` | boolean | True if length is 0 |
| `strPadLeft` | `strPadLeft(s, width, char)` | string | Left-pad s with char to reach width |
| `strPadRight` | `strPadRight(s, width, char)` | string | Right-pad s with char to reach width |
| `strChars` | `strChars(s)` | array | Array of individual characters |

```larv
import "string"

print(strUpper("hello"))               // HELLO
print(strSplit("a,b,c", ","))         // [a, b, c]
print(strJoin(["x", "y", "z"], "-"))  // x-y-z
print(strPadLeft("5", 3, "0"))        // 005
```

---

### 15.3 `io`

```larv
import "io"
```

File paths can be absolute or relative to the current working directory.

| Function | Signature | Returns | Description |
|---|---|---|---|
| `readFile` | `readFile(path)` | string | Read entire file as a UTF-8 string |
| `writeFile` | `writeFile(path, content)` | nil | Write string to file, replacing contents |
| `appendFile` | `appendFile(path, content)` | nil | Append string to end of file |
| `readLines` | `readLines(path)` | array | Array of lines (strings) |
| `readBytes` | `readBytes(path)` | array | Raw byte array |
| `writeBytes` | `writeBytes(path, bytes)` | nil | Write byte array to file |
| `deleteFile` | `deleteFile(path)` | nil | Delete the file at path |
| `fileExists` | `fileExists(path)` | boolean | True if the path exists as a file |
| `isDir` | `isDir(path)` | boolean | True if the path is a directory |
| `listDir` | `listDir(path)` | array | File names in the directory |
| `makeDir` | `makeDir(path)` | nil | Create directory (including parents) |
| `copyFile` | `copyFile(src, dst)` | nil | Copy a file |
| `moveFile` | `moveFile(src, dst)` | nil | Move or rename a file |
| `fileSize` | `fileSize(path)` | number | File size in bytes |
| `cwd` | `cwd()` | string | Current working directory path |
| `absPath` | `absPath(path)` | string | Absolute path of the given path |

```larv
import "io"

writeFile("log.txt", "First line\n")
appendFile("log.txt", "Second line\n")
var content = readFile("log.txt")
print(content)

var entries = listDir(".")
for entry in entries {
    print(entry)
}
```

---

### 15.4 `list`

```larv
import "list"
```

A functional, index-oriented API for dynamic lists. Lists created with `listNew()` or array literals are interchangeable.

| Function | Signature | Returns | Description |
|---|---|---|---|
| `listNew` | `listNew()` | array | Create empty list |
| `listAdd` | `listAdd(list, value)` | nil | Append value |
| `listAddAt` | `listAddAt(list, index, value)` | nil | Insert value at index |
| `listRemove` | `listRemove(list, index)` | removed value | Remove element at index |
| `listGet` | `listGet(list, index)` | value | Get element at index |
| `listSet` | `listSet(list, index, value)` | nil | Replace element at index |
| `listSize` | `listSize(list)` | number | Number of elements |
| `listContains` | `listContains(list, value)` | boolean | True if value is present |
| `listIndexOf` | `listIndexOf(list, value)` | number | First index of value, or -1 |
| `listSlice` | `listSlice(list, from, to)` | array | Sub-list from to to (exclusive) |
| `listReverse` | `listReverse(list)` | nil | Reverse in place |
| `listSort` | `listSort(list)` | nil | Sort in place (natural order) |
| `listConcat` | `listConcat(a, b)` | array | New list: a followed by b |
| `listFlat` | `listFlat(list)` | array | Flatten one level of nested arrays |
| `listUnique` | `listUnique(list)` | array | New list with duplicates removed |
| `listFill` | `listFill(value, n)` | array | New list with value repeated n times |
| `listClear` | `listClear(list)` | nil | Remove all elements |
| `listIsEmpty` | `listIsEmpty(list)` | boolean | True if empty |
| `listFirst` | `listFirst(list)` | value | First element |
| `listLast` | `listLast(list)` | value | Last element |
| `listPop` | `listPop(list)` | value | Remove and return last element |
| `listShuffle` | `listShuffle(list)` | nil | Shuffle in place (random order) |

```larv
import "list"

var nums = listNew()
listAdd(nums, 3)
listAdd(nums, 1)
listAdd(nums, 2)
listSort(nums)
print(nums)   // [1, 2, 3]

var unique = listUnique([1, 2, 2, 3, 3, 3])
print(unique)   // [1, 2, 3]
```

---

### 15.5 `map`

```larv
import "map"
```

A string-keyed hash map. Keys must be strings.

| Function | Signature | Returns | Description |
|---|---|---|---|
| `mapNew` | `mapNew()` | map | Create empty map |
| `mapSet` | `mapSet(map, key, value)` | nil | Set key to value |
| `mapGet` | `mapGet(map, key)` | value or nil | Get value for key |
| `mapHas` | `mapHas(map, key)` | boolean | True if key exists |
| `mapRemove` | `mapRemove(map, key)` | nil | Remove a key |
| `mapSize` | `mapSize(map)` | number | Number of key-value pairs |
| `mapKeys` | `mapKeys(map)` | array | Array of all keys |
| `mapValues` | `mapValues(map)` | array | Array of all values |
| `mapClear` | `mapClear(map)` | nil | Remove all entries |
| `mapIsEmpty` | `mapIsEmpty(map)` | boolean | True if empty |
| `mapMerge` | `mapMerge(a, b)` | map | New map: a merged with b (b wins on conflict) |
| `mapContainsValue` | `mapContainsValue(map, value)` | boolean | True if any key maps to value |
| `mapToList` | `mapToList(map)` | array | Array of `[key, value]` pairs |

```larv
import "map"

var scores = mapNew()
mapSet(scores, "Alice", 95)
mapSet(scores, "Bob", 87)
print(mapGet(scores, "Alice"))    // 95
print(mapKeys(scores))            // [Alice, Bob]
print(mapSize(scores))            // 2
```

---

### 15.6 `http`

```larv
import "http"
```

All functions return a map with the following keys:

| Key | Type | Description |
|---|---|---|
| `status` | number | HTTP status code (200, 404, etc.) |
| `body` | string | Response body as a string |
| `ok` | boolean | True if status is in 200–299 range |

| Function | Signature | Description |
|---|---|---|
| `httpGet` | `httpGet(url)` | GET request |
| `httpPost` | `httpPost(url, body)` | POST with plain-text body |
| `httpPostJson` | `httpPostJson(url, body)` | POST with `application/json` content type |
| `httpPut` | `httpPut(url, body)` | PUT request |
| `httpDelete` | `httpDelete(url)` | DELETE request |
| `httpRequest` | `httpRequest(url, method, contentType, body, headers)` | Fully custom request |

```larv
import "http"
import "map"

var resp = httpGet("https://api.example.com/data")
if resp["ok"] {
    print(resp["body"])
} else {
    printErr("HTTP " + resp["status"])
}

var headers = mapNew()
mapSet(headers, "Authorization", "Bearer mytoken")
var r = httpRequest("https://api.example.com/secure", "GET", nil, nil, headers)
```

---

### 15.7 `regex`

```larv
import "regex"
```

Uses Java regular expression syntax (`java.util.regex`).

| Function | Signature | Returns | Description |
|---|---|---|---|
| `regexMatch` | `regexMatch(input, pattern)` | boolean | True if entire input matches pattern |
| `regexTest` | `regexTest(input, pattern)` | boolean | True if pattern is found anywhere in input |
| `regexFind` | `regexFind(input, pattern)` | string or nil | First matching substring |
| `regexFindAll` | `regexFindAll(input, pattern)` | array | All non-overlapping matches |
| `regexReplace` | `regexReplace(input, pattern, replacement)` | string | Replace first match |
| `regexReplaceAll` | `regexReplaceAll(input, pattern, replacement)` | string | Replace all matches |
| `regexSplit` | `regexSplit(input, pattern)` | array | Split on pattern |
| `regexGroup` | `regexGroup(input, pattern, groupIndex)` | string or nil | Capture group by index (1-based) |
| `regexGroups` | `regexGroups(input, pattern)` | array | All capture groups from the first match |

```larv
import "regex"

print(regexTest("hello123", "\\d+"))              // true
print(regexFind("2026-05-18", "\\d{4}"))          // 2026
print(regexFindAll("cat bat hat", "[a-z]at"))     // [cat, bat, hat]
print(regexReplaceAll("foo bar foo", "foo", "x")) // x bar x

var groups = regexGroups("2026-05-18", "(\\d{4})-(\\d{2})-(\\d{2})")
print(groups)   // [2026, 05, 18]
```

---

### 15.8 `date`

```larv
import "date"
```

Date strings use `"yyyy-MM-dd"` format by default. Datetime strings use `"yyyy-MM-dd HH:mm:ss"`.

Time units for arithmetic functions: `"seconds"`, `"minutes"`, `"hours"`, `"days"`, `"weeks"`, `"months"`, `"years"`.

| Function | Signature | Returns | Description |
|---|---|---|---|
| `timestamp` | `timestamp()` | number | Unix epoch time in milliseconds |
| `dateNow` | `dateNow()` | string | Today's date as `"yyyy-MM-dd"` |
| `timeNow` | `timeNow()` | string | Current time as `"HH:mm:ss"` |
| `dateTimeNow` | `dateTimeNow()` | string | Current datetime as `"yyyy-MM-dd HH:mm:ss"` |
| `dateFormat` | `dateFormat(ts, pattern)` | string | Format a millisecond timestamp with a Java pattern |
| `dateParse` | `dateParse(str, pattern)` | number | Parse a date string to a millisecond timestamp |
| `dateAdd` | `dateAdd(str, amount, unit)` | string | Add amount of unit to a date string |
| `dateSub` | `dateSub(str, amount, unit)` | string | Subtract amount of unit from a date string |
| `dateDiff` | `dateDiff(a, b, unit)` | number | Difference between date strings in unit |
| `dayOfWeek` | `dayOfWeek(str)` | string | Day name (e.g. `"Monday"`) |
| `monthName` | `monthName(str)` | string | Month name (e.g. `"January"`) |
| `year` | `year(str)` | number | Extract the year |
| `month` | `month(str)` | number | Extract the month (1–12) |
| `day` | `day(str)` | number | Extract the day of month |
| `hour` | `hour(str)` | number | Extract the hour |
| `minute` | `minute(str)` | number | Extract the minute |
| `second` | `second(str)` | number | Extract the second |
| `isBefore` | `isBefore(a, b)` | boolean | True if date a is before b |
| `isAfter` | `isAfter(a, b)` | boolean | True if date a is after b |

```larv
import "date"

print(dateNow())                            // 2026-05-18
print(dateAdd("2026-01-01", 30, "days"))    // 2026-01-31
print(dateDiff("2026-01-01", "2026-12-31", "days"))   // 364
print(dayOfWeek("2026-05-18"))              // Monday
print(year("2026-05-18"))                   // 2026
```

---

### 15.9 `encode`

```larv
import "encode"
```

(Also importable as `import "base64"` for backward compatibility.)

| Function | Signature | Returns | Description |
|---|---|---|---|
| `base64Encode` | `base64Encode(str)` | string | Standard Base64 encoding |
| `base64Decode` | `base64Decode(str)` | string | Standard Base64 decoding |
| `base64EncodeUrl` | `base64EncodeUrl(str)` | string | URL-safe Base64 encoding |
| `base64DecodeUrl` | `base64DecodeUrl(str)` | string | URL-safe Base64 decoding |
| `hashMd5` | `hashMd5(str)` | string | MD5 hex digest |
| `hashSha1` | `hashSha1(str)` | string | SHA-1 hex digest |
| `hashSha256` | `hashSha256(str)` | string | SHA-256 hex digest |
| `hashSha512` | `hashSha512(str)` | string | SHA-512 hex digest |
| `hexEncode` | `hexEncode(str)` | string | Hex-encode UTF-8 bytes |
| `hexDecode` | `hexDecode(hex)` | string | Decode hex string to UTF-8 |
| `urlEncode` | `urlEncode(str)` | string | Percent-encode for URLs |
| `urlDecode` | `urlDecode(str)` | string | Decode percent-encoded string |

```larv
import "encode"

var encoded = base64Encode("Hello, World!")
print(encoded)                   // SGVsbG8sIFdvcmxkIQ==
print(base64Decode(encoded))     // Hello, World!

print(hashSha256("password"))    // 64-char hex string
print(urlEncode("a b+c"))        // a+b%2Bc
```

---

### 15.10 `convert`

```larv
import "convert"
```

Type conversion and introspection utilities.

| Function | Signature | Returns | Description |
|---|---|---|---|
| `toNumber` | `toNumber(value)` | number | Convert string or boolean to number |
| `toString` | `toString(value)` | string | Convert any value to its string representation |
| `toBool` | `toBool(value)` | boolean | Convert string/number to boolean; `"true"/"yes"/"1"/"on"` → true |
| `toInt` | `toInt(n)` | number | Truncate float to integer (toward zero) |
| `toHex` | `toHex(n)` | string | Integer to lowercase hex string |
| `toOctal` | `toOctal(n)` | string | Integer to octal string |
| `toBinary` | `toBinary(n)` | string | Integer to binary string |
| `fromHex` | `fromHex(str)` | number | Hex string to number |
| `fromOctal` | `fromOctal(str)` | number | Octal string to number |
| `fromBinary` | `fromBinary(str)` | number | Binary string to number |
| `toBytes` | `toBytes(str)` | array | UTF-8 string to byte array |
| `fromBytes` | `fromBytes(bytes)` | string | Byte array to UTF-8 string |
| `typeOf` | `typeOf(value)` | string | Runtime type name |

```larv
import "convert"

print(toNumber("42"))      // 42
print(toInt(9.99))         // 9
print(toHex(255))          // ff
print(toBinary(10))        // 1010
print(fromHex("ff"))       // 255
print(typeOf("hello"))     // String
print(typeOf(42))          // Number
print(typeOf([]))          // Array
```

---

### 15.11 `system`

```larv
import "system"
```

OS and process interaction.

| Function | Signature | Returns | Description |
|---|---|---|---|
| `exit` | `exit(code?)` | — | Terminate the process with exit code (default 0) |
| `getEnv` | `getEnv(name)` | string or nil | Read an environment variable |
| `getArgs` | `getArgs()` | array | Command-line arguments passed to the program |
| `clock` | `clock()` | number | Elapsed JVM time in milliseconds |
| `nanoTime` | `nanoTime()` | number | Elapsed JVM time in nanoseconds |
| `sleep` | `sleep(ms)` | nil | Pause execution for ms milliseconds |
| `exec` | `exec(cmd)` | map | Run shell command; returns `{exit, out, err, ok}` |
| `osName` | `osName()` | string | OS name (e.g. `"Linux"`) |
| `osArch` | `osArch()` | string | CPU architecture (e.g. `"amd64"`) |
| `freeMemory` | `freeMemory()` | number | Free JVM heap memory in bytes |
| `totalMemory` | `totalMemory()` | number | Total JVM heap memory in bytes |
| `gc` | `gc()` | nil | Suggest a garbage collection cycle |

`exec()` returns a map with:
- `exit` — exit code (number)
- `out` — stdout output (string)
- `err` — stderr output (string)
- `ok` — true if exit code is 0 (boolean)

```larv
import "system"

print(osName())    // Linux

var result = exec("ls -la")
if result["ok"] {
    print(result["out"])
} else {
    printErr(result["err"])
}

sleep(1000)
print("1 second later")
```

---

### 15.12 `properties`

```larv
import "properties"
```

Read and write Java `.properties` files (key=value pairs).

| Function | Signature | Returns | Description |
|---|---|---|---|
| `loadProp` | `loadProp(file)` | nil | Load a `.properties` file into memory |
| `getProp` | `getProp(key)` | string or nil | Get a property value by key |
| `setProp` | `setProp(file, key, value)` | nil | Set a property value |
| `saveProp` | `saveProp(file)` | nil | Save current properties to file |
| `getAllProps` | `getAllProps()` | map | Map of all loaded properties |

```larv
import "properties"

loadProp("config.properties")
print(getProp("database.host"))    // e.g. "localhost"

setProp("config.properties", "app.version", "2.0")
saveProp("config.properties")

var all = getAllProps()
print(all)
```

---

## 16. Error Types

The interpreter distinguishes three error kinds in its error messages:

| Kind | Trigger |
|---|---|
| `LEXER` | Invalid characters, unterminated strings or raw strings |
| `PARSER` | Unexpected tokens, missing braces/parentheses, malformed statements |
| `RUNTIME` | Type errors, undefined variables, index out of bounds, division by zero, failed I/O |
| `FFI` | Java class not found, method not found, argument type mismatch |

Errors display with red-colored output to `stderr` in the format:

```
[Kind] Message (line L, col C)
```

---

## 17. Execution Pipeline

Every Larv program passes through four stages:

```
Source Text
    │
    ▼
[Lexer] → List<Token>
    │
    ▼
[Parser] → List<Statement> (AST)
    │
    ▼
[Interpreter]
    ├── StatementExecutor  (executes statements)
    ├── ExpressionEvaluator (evaluates expressions)
    ├── Environment         (variable scoping)
    ├── NativeRegistry      (core built-ins)
    ├── NativeLibraryLoader (stdlib on demand)
    └── JavaClassRegistry   (FFI)
```

1. **Lexer** (`Lexer.java`) — tokenises the source string into a flat `List<Token>`, tracking line and column for every token.
2. **Parser** — a two-phase Pratt parser: `ExpressionParser` handles expressions with operator precedence; `StatementParser` handles declarations, control flow, and other statements.
3. **Interpreter** — tree-walking interpreter: `StatementExecutor` visits each `Statement`, `ExpressionEvaluator` evaluates each `Expression`. Scope is managed by a chain of `Environment` objects. Signals (`ReturnSignal`, `BreakSignal`, `ContinueSignal`, `ThrowSignal`) are used to implement non-local control flow.
4. **Standard library** — each stdlib module is loaded lazily the first time its `import` statement is executed, registering native Java callbacks into the execution context.