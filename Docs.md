# Larv Language Reference

**Version:** 1.0.0-beta

This document is the complete technical reference for the Larv programming language. For a quick introduction, see the [README](./README.md).

---

## Table of Contents

1. [Lexical Structure](#1-lexical-structure)
2. [Types](#2-types)
3. [Expressions](#3-expressions)
4. [Statements](#4-statements)
5. [Functions](#5-functions)
6. [Classes](#6-classes)
7. [Arrays](#7-arrays)
8. [Control Flow](#8-control-flow)
9. [Module System](#9-module-system)
10. [Java Interop (FFI)](#10-java-interop-ffi)
11. [Built-in Functions](#11-built-in-functions)
12. [Standard Library — math](#12-standard-library--math)
13. [Standard Library — string](#13-standard-library--string)
14. [Standard Library — list](#14-standard-library--list)
15. [Standard Library — map](#15-standard-library--map)
16. [Standard Library — io](#16-standard-library--io)
17. [Standard Library — http](#17-standard-library--http)
18. [Standard Library — system](#18-standard-library--system)
19. [Error Handling](#19-error-handling)
20. [Grammar Summary](#20-grammar-summary)

---

## 1. Lexical Structure

### Source encoding

Source files are read as UTF-8 text. The conventional extension is `.larv`.

### Comments

```larv
// This is a comment. Everything from // to end-of-line is ignored.
```

There are no block comments.

### Whitespace

Spaces, tabs, and carriage returns are ignored. Newlines advance the line counter (used in error messages) but are not significant tokens.

### Identifiers

Identifiers start with a letter or underscore and continue with letters, digits, or underscores. They are case-sensitive.

```
identifier → [A-Za-z_][A-Za-z0-9_]*
```

### Keywords

The following words are reserved and cannot be used as identifiers:

```
var   const   print   if     else    while   for    func
return  break   continue  class   new    this    nil
true    false   include   from    involve  import
```

### Number literals

```
number → [0-9]+ ('.' [0-9]+)?
```

All numbers are internally 64-bit floating-point (`double`). Both `42` and `3.14` are valid.

### String literals

Strings are enclosed in double quotes:

```
string → '"' character* '"'
```

Supported escape sequences:

| Escape | Character     |
|--------|---------------|
| `\"`   | Double quote  |
| `\\`   | Backslash     |
| `\n`   | Newline       |
| `\t`   | Tab           |
| `\r`   | Carriage return|

A `"` that appears inside balanced parentheses within a string is treated as a literal character, not the closing delimiter. This allows `involve` constructor args to be written naturally:

```larv
include fw from "java.io.FileWriter" involve { "example.txt" }
```

### Operators and punctuation

```
+  -  *  /  ++  --
=  ==  !=  <  >  <=  >=
(  )  {  }  [  ]  ,  ;  :  .
```

`!` alone (not followed by `=`) is a lexer error. Use `!= ` for inequality.

---

## 2. Types

Larv is dynamically typed. Every value at runtime is one of:

| Type    | Underlying Java type     | Literal / source               |
|---------|--------------------------|--------------------------------|
| Number  | `double`                 | `42`, `3.14`, arithmetic result|
| String  | `java.lang.String`       | `"hello"`                      |
| Boolean | `boolean`                | `true`, `false`                |
| Array   | `java.util.List<Object>` | `[1, 2, 3]`                    |
| Map     | `java.util.LinkedHashMap`| created by `mapNew()`          |
| Object  | `LarvObject`             | `new ClassName(...)`           |
| Nil     | `null`                   | `nil`                          |

### Truthiness

The following values are falsy; everything else is truthy:

- `false`
- `nil`

Numbers, strings (including `""`), arrays, maps, and objects are all truthy.

---

## 3. Expressions

Expressions are evaluated using a Pratt (top-down operator precedence) parser.

### Precedence table (lowest → highest)

| Precedence | Operators             |
|------------|-----------------------|
| 1          | `=` (assignment)      |
| 2          | `==`, `!=`            |
| 3          | `<`, `>`, `<=`, `>=`  |
| 4          | `+`, `-`              |
| 5          | `*`, `/`              |
| 6          | Unary `-`             |
| 7          | `.` (member access), `()` (call), `[]` (index) |

### Literal expressions

```larv
42          // number
3.14        // number
"hello"     // string
true        // boolean
false       // boolean
nil         // nil value
[1, 2, 3]  // array literal
```

### Variable expressions

```larv
x           // read variable x
```

### Assignment expressions

```larv
x = 10            // assign to variable
obj.field = val   // set field on object
arr[i] = val      // set array element
```

### Binary expressions

```larv
a + b    a - b    a * b    a / b
a == b   a != b   a < b    a > b   a <= b   a >= b
"foo" + "bar"   // string concatenation
```

### Unary expression

```larv
-x        // numeric negation
```

### Call expression

```larv
func(a, b, c)        // call function
obj.method(a, b)     // call method on object or Java bound class
```

### Member access

```larv
obj.field     // read a field or call a method
obj.method()  // method call
```

### Index expression

```larv
arr[0]          // array element
arr[i + 1]      // computed index
```

### `this` expression

Only valid inside a class method. Refers to the current instance.

```larv
this.name = "value"
print this.x
```

### `new` expression

Creates an instance of a user-defined class:

```larv
new ClassName(arg1, arg2)
```

### Grouping

```larv
(a + b) * c
```

### Class reference

Inside method bodies, referring to the class name provides access to static-style members (if any).

---

## 4. Statements

Every statement is terminated by the end of its syntactic structure (no mandatory `;` for most forms). Semicolons are optional and silently consumed where they appear.

### Variable declaration — `var`

```larv
var name = expression
var x               // declared with nil value if no initializer
```

### Constant declaration — `const`

```larv
const PI = 3.14159
```

`const` requires an initializer. Reassigning a constant is a runtime error.

### Print statement

```larv
print expression
```

Equivalent to `print(expression)`.

### Expression statement

Any expression used as a standalone statement:

```larv
add(1, 2)
i++
i--
obj.method()
```

### Increment / decrement

```larv
i++    // equivalent to i = i + 1
i--    // equivalent to i = i - 1
```

These are statement forms only; they cannot appear inside a larger expression.

### Assignment statement

```larv
x = newValue
obj.field = newValue
arr[i] = newValue
```

---

## 5. Functions

### Declaration

```larv
func name(param1, param2, ...) {
    // body
    return value
}
```

- Parameters are positional; there are no default values or named parameters.
- `return` exits the function and optionally returns a value. A function that falls off the end returns `nil`.
- Functions are values; they can be stored in variables and passed as arguments.

### Calling

```larv
name(arg1, arg2)
```

### Recursion

```larv
func fib(n) {
    if n <= 1 { return n }
    return fib(n - 1) + fib(n - 2)
}
```

### First-class functions

```larv
func square(x) { return x * x }
var fn = square
print fn(5)    // 25
```

### Closures

Functions close over their enclosing scope:

```larv
func makeCounter() {
    var count = 0
    func increment() {
        count = count + 1
        return count
    }
    return increment
}
var c = makeCounter()
print c()   // 1
print c()   // 2
```

---

## 6. Classes

### Declaration

```larv
class ClassName {
    func methodName(params) {
        // body
    }
}
```

A class body may only contain function declarations. Field declarations are implicit — fields are set by assigning to `this.fieldName` inside any method.

### Construction

```larv
var obj = new ClassName(arg1, arg2)
```

Larv does not have a special constructor syntax. By convention, define an `init` method and call it explicitly, or pass initialization args to any method you name for setup. The `new` expression calls the runtime to create an empty `LarvObject` instance and looks for a method matching the class name's structure.

**Pattern: use an `init` method**

```larv
class Point {
    func init(x, y) {
        this.x = x
        this.y = y
    }
    func toString() {
        return "(" + this.x + ", " + this.y + ")"
    }
}
var p = new Point(3, 4)
p.init(3, 4)
print p.toString()   // (3, 4)
```

### Fields

Fields are created by assignment on `this`. There is no field declaration syntax.

```larv
this.name = "value"
print this.name
```

### Methods

Methods are functions declared inside a class body. They are called with dot syntax on an instance.

```larv
obj.methodName(arg1, arg2)
```

Inside a method, `this` is automatically bound to the receiver.

### Inheritance

Not supported in the current version.

---

## 7. Arrays

### Literal syntax

```larv
var arr = [1, 2, 3]
var empty = []
var nested = [[1, 2], [3, 4]]
```

### Index access

```larv
arr[0]        // read (zero-based)
arr[i]        // computed index
```

### Index assignment

```larv
arr[0] = 99
```

### `len` built-in

```larv
len(arr)      // returns number of elements
```

### Dot-method API

These methods are called on any array value with dot syntax:

| Method                | Returns   | Description                              |
|-----------------------|-----------|------------------------------------------|
| `.push(val)`          | nil       | Append val to end                        |
| `.pop()`              | any       | Remove and return last element           |
| `.peek()`             | any       | Read last element without removing       |
| `.first()`            | any       | Read first element                       |
| `.last()`             | any       | Read last element                        |
| `.contains(val)`      | boolean   | True if val is in array                  |
| `.indexOf(val)`       | number    | Index of first occurrence, -1 if missing |
| `.isEmpty()`          | boolean   | True if length is 0                      |
| `.clear()`            | nil       | Remove all elements                      |
| `.reverse()`          | nil       | Reverse in place                         |
| `.remove(i)`          | any       | Remove and return element at index i     |
| `.slice(from, to)`    | array     | New sub-array [from, to)                 |
| `.join(sep)`          | string    | Join all elements with separator         |

---

## 8. Control Flow

### `if` / `else`

```larv
if condition {
    // then branch
}

if condition {
    // then
} else {
    // else
}
```

The condition does not require parentheses. Braces are required around both branches.

### `while`

```larv
while condition {
    // body
}
```

### `for` (C-style)

```larv
for init; condition; increment {
    // body
}
```

`init` can be a variable assignment (`i = 0`) or an expression statement.  
`increment` can be `i++`, `i--`, or an expression statement.

```larv
for i = 0; i < 10; i++ {
    print i
}
```

### `for` (foreach)

Iterates over every element of an array:

```larv
for element : array {
    // body — element is bound to each value
}
```

```larv
var fruits = ["apple", "banana", "cherry"]
for fruit : fruits {
    print fruit
}
```

### `break`

Exits the innermost loop immediately.

```larv
while true {
    if done { break }
}
```

### `continue`

Skips the rest of the current loop iteration and advances to the next.

```larv
for i = 0; i < 10; i++ {
    if i == 5 { continue }
    print i
}
```

---

## 9. Module System

### Standard library import

```larv
import math
import string
import list
import map
import io
import http
import system
```

After an import, all functions from that library are registered in the global scope and can be called by name.

### File import

```larv
import "utils.larv"
import "lib/helpers.larv"
```

The path is relative to the directory of the importing file. The imported file is parsed and executed in a shared environment, so any `func` or `var` defined at the top level becomes available to the importer.

Import paths use forward slashes and include the `.larv` extension.

---

## 10. Java Interop (FFI)

The FFI system lets Larv scripts bind and call any Java class on the JVM classpath.

### Static binding

```larv
include Alias from "fully.qualified.ClassName"
```

The alias becomes a global name. Calling `Alias.method(args)` invokes the corresponding public method on the class. If the class has a no-argument constructor, an instance is automatically created; otherwise only static methods are available.

```larv
include Math from "java.lang.Math"
print Math.sqrt(25)      // 5.0
print Math.floor(3.9)    // 3.0
```

### Instance binding with `involve`

```larv
include Alias from "fully.qualified.ClassName" involve { "arg1", "arg2" }
```

`involve { ... }` passes constructor arguments. After binding, the alias holds a live Java instance and both instance and static methods are callable.

```larv
include sb from "java.lang.StringBuilder"
sb.append("Hello, ")
sb.append("World!")
print sb.toString()    // Hello, World!
```

```larv
include fw from "java.io.FileWriter" involve { "out.txt" }
fw.write("data line\n")
fw.flush()
fw.close()
```

### Argument resolution in `involve`

Inside `involve { ... }` all args are quoted strings. They are resolved as follows:

- `"hello"` → passed as a `String` (or coerced to match the constructor signature)
- `"42"` → coerced to `int`, `long`, or `double` as needed
- `"java.lang.System.in"` → resolved to the public static field `System.in`
- `"some.ClassName.field"` → resolved to any public static field
- `"some.ClassName(arg)"` → constructs that class with the given literal args

```larv
// Wrap System.in in a Scanner
include scanner from "java.util.Scanner" involve { "java.lang.System.in" }
var line = scanner.nextLine()
print line
```

### Calling methods

All public methods of the bound class are accessible:

```larv
include list from "java.util.ArrayList"
list.add("first")
list.add("second")
print list.size()     // 2
print list.get(0)     // first
```

### Field access

Public fields on a bound instance can be read with dot syntax:

```larv
include point from "java.awt.Point" involve { "3", "4" }
print point.x   // 3
print point.y   // 4
```

---

## 11. Built-in Functions

Always available without any import:

### `print(value)`

Converts `value` to a string and prints it to standard output followed by a newline.

```larv
print("hello")
print(42)
print(true)
print(nil)    // prints "nil"
```

### `input()`

Reads a line from standard input and returns it as a string (without the trailing newline).

```larv
var name = input()
print "You typed: " + name
```

### `len(array)`

Returns the number of elements in an array as a number.

```larv
var arr = [1, 2, 3]
print len(arr)    // 3
```

Passing a non-array raises a runtime error.

---

## 12. Standard Library — math

```larv
import math
```

### Constants

| Function  | Value            |
|-----------|------------------|
| `pi()`    | 3.141592653589793 |
| `e()`     | 2.718281828459045 |

### Basic operations

```larv
sqrt(16)        // 4.0
pow(2, 10)      // 1024.0
abs(-5)         // 5.0
floor(3.7)      // 3.0
ceil(3.2)       // 4.0
round(3.5)      // 4.0
max(3, 7)       // 7.0
min(3, 7)       // 3.0
toInt(3.9)      // 3.0  (truncate toward zero)
```

### Logarithms and exponents

```larv
log(2.71828)    // ≈ 1.0 (natural log)
log10(100)      // 2.0
```

### Trigonometry (angles in radians)

```larv
sin(pi() / 2)   // 1.0
cos(0)          // 1.0
tan(pi() / 4)   // ≈ 1.0
asin(1)         // ≈ 1.5707 (π/2)
acos(1)         // 0.0
atan(1)         // ≈ 0.7854 (π/4)
atan2(1, 1)     // ≈ 0.7854
toRadians(180)  // ≈ 3.14159
toDegrees(pi()) // 180.0
```

### Random numbers

```larv
random()            // float in [0, 1)
randomInt(1, 6)     // integer in [1, 6] inclusive
```

### Utility

```larv
clamp(15, 0, 10)    // 10 (clamped to max)
clamp(-5, 0, 10)    // 0  (clamped to min)
sign(-3)            // -1.0
sign(0)             // 0.0
sign(7)             // 1.0
isNaN(0 / 0)        // true (note: Larv may propagate NaN from Java)
isInfinite(1 / 0)   // true (same caveat)
```

---

## 13. Standard Library — string

```larv
import string
```

### Length and case

```larv
strLen("hello")       // 5
strUpper("hello")     // "HELLO"
strLower("WORLD")     // "world"
```

### Whitespace

```larv
strTrim("  hi  ")       // "hi"
strTrimLeft("  hi  ")   // "hi  "
strTrimRight("  hi  ")  // "  hi"
strIsEmpty("  ")        // true
strIsEmpty("x")         // false
```

### Searching

```larv
strContains("hello world", "world")    // true
strStartsWith("hello", "hel")          // true
strEndsWith("hello", "llo")            // true
strIndexOf("hello", "l")               // 2  (first occurrence)
```

### Slicing and editing

```larv
strSlice("hello", 1, 4)              // "ell"  ([from, to))
strCharAt("hello", 0)                // "h"
strReplace("aabbaa", "aa", "XX")     // "XXbbaa"  (first only)
strReplaceAll("aabbaa", "a", "X")    // "XXbbXX"
```

### Splitting and joining

```larv
strSplit("a,b,c", ",")    // ["a", "b", "c"]
strJoin(["a", "b"], "-")  // "a-b"
```

### Padding and repeating

```larv
strRepeat("ab", 3)              // "ababab"
strPadLeft("42", 5, "0")        // "00042"
strPadRight("hi", 5, ".")       // "hi..."
```

### Conversion

```larv
strToNumber("3.14")      // 3.14
strFromNumber(3.14)      // "3.14"
strFromNumber(42)        // "42"  (no trailing .0 for whole numbers)
```

### Characters

```larv
strReverse("hello")      // "olleh"
strChars("abc")          // ["a", "b", "c"]
```

---

## 14. Standard Library — list

```larv
import list
```

The `list` library provides an alternative functional API for arrays. Arrays created with `listNew()` and array literals (`[...]`) are the same underlying type and are interchangeable.

### Creation

```larv
var l = listNew()           // empty list
var filled = listFill(0, 5) // [0, 0, 0, 0, 0]
```

### Adding and removing

```larv
listAdd(l, "value")            // append
listAddAt(l, 0, "front")       // insert at index
listRemove(l, 0)               // remove at index
listClear(l)                   // remove all
var last = listPop(l)          // remove and return last
```

### Reading

```larv
listGet(l, 0)                  // element at index 0
listFirst(l)                   // first element
listLast(l)                    // last element
listSize(l)                    // element count
listIsEmpty(l)                 // true if size == 0
```

### Searching

```larv
listContains(l, "x")           // true / false
listIndexOf(l, "x")            // index or -1
```

### Transformations (return new lists)

```larv
listSlice(l, 1, 3)             // sub-list [from, to)
listReverse(l)                 // reversed copy
listSort(l)                    // sorted copy (numbers or strings)
listConcat(a, b)               // new list = a + b
listFlat(nested)               // flatten one level
listUnique(l)                  // deduplicated copy
```

### In-place

```larv
listSet(l, 0, "new")           // overwrite element at index
listShuffle(l)                 // shuffle in place
```

---

## 15. Standard Library — map

```larv
import map
```

Maps store key-value pairs. Keys must be strings. Insertion order is preserved.

### Creation

```larv
var m = mapNew()
```

### Setting and getting

```larv
mapSet(m, "name", "Larv")
mapSet(m, "version", 1)
print mapGet(m, "name")       // "Larv"
print mapGet(m, "missing")    // nil
```

### Querying

```larv
mapHas(m, "name")             // true
mapSize(m)                    // 2
mapIsEmpty(m)                 // false
mapContainsValue(m, "Larv")   // true
```

### Removal

```larv
mapRemove(m, "name")
mapClear(m)
```

### Iteration

```larv
var keys   = mapKeys(m)        // array of strings
var values = mapValues(m)      // array of values

var pairs  = mapToList(m)      // array of [key, value] arrays
for pair : pairs {
    print pair[0] + " => " + pair[1]
}
```

### Merging

```larv
var merged = mapMerge(a, b)    // b's keys overwrite a's on conflict
```

---

## 16. Standard Library — io

```larv
import io
```

### Reading files

```larv
var text  = readFile("data.txt")          // entire file as string
var lines = readLines("data.txt")         // array of lines
var bytes = readBytes("image.png")        // array of byte values (0–255)
```

### Writing files

```larv
writeFile("out.txt", "hello\n")           // overwrite
appendFile("log.txt", "new line\n")       // append
writeBytes("out.bin", bytes)              // write byte array
```

### File and directory queries

```larv
fileExists("path/to/file")    // boolean
isDir("path/to/dir")          // boolean
fileSize("file.txt")          // size in bytes
listDir(".")                  // array of entry paths
```

### Directory and file management

```larv
makeDir("new/nested/dir")              // creates all parent dirs
copyFile("src.txt", "dst.txt")         // copy (overwrites dst)
moveFile("old.txt", "new.txt")         // move / rename
deleteFile("unwanted.txt")             // returns true if deleted
```

### Path utilities

```larv
cwd()                    // current working directory
absPath("relative.txt")  // absolute path string
```

---

## 17. Standard Library — http

```larv
import http
```

All functions return a map with these keys:

| Key      | Type    | Description              |
|----------|---------|--------------------------|
| `status` | number  | HTTP status code         |
| `body`   | string  | Response body            |
| `ok`     | boolean | True if status is 2xx    |

Timeout for all requests is 30 seconds. Redirects are followed automatically.

### GET

```larv
var res = httpGet("https://api.example.com/data")
if res["ok"] {
    print res["body"]
}
```

### POST

```larv
var res = httpPost("https://api.example.com/submit", "raw body text")
```

### POST with JSON

```larv
var res = httpPostJson("https://api.example.com/json", "{\"key\":\"value\"}")
print res["status"]
```

### PUT

```larv
var res = httpPut("https://api.example.com/resource/1", "updated body")
```

### DELETE

```larv
var res = httpDelete("https://api.example.com/resource/1")
print res["ok"]
```

### Custom request

```larv
var res = httpRequest("PATCH", "https://api.example.com/item", "{}", "application/json")
```

Signature: `httpRequest(method, url, body, contentType)`  
A fifth argument (a map of string → string) adds custom headers.

---

## 18. Standard Library — system

```larv
import system
```

### Process control

```larv
exit(0)       // exit with code 0
exit(1)       // exit with code 1 (error)
```

### Environment

```larv
var home = getEnv("HOME")     // nil if not set
var args = getArgs()          // array of CLI args (may be empty)
```

### Time

```larv
var start = clock()           // milliseconds since Unix epoch
var t     = nanoTime()        // nanoseconds (monotonic clock)
sleep(1000)                   // pause for 1 second
```

### Shell execution

```larv
var result = exec("ls -la")
print result["exit"]    // exit code (number)
print result["out"]     // stdout (string)
print result["err"]     // stderr (string)
print result["ok"]      // true if exit == 0
```

`exec` runs via `sh -c` on Unix-like systems.

### JVM information

```larv
print osName()          // e.g. "Linux", "Mac OS X"
print osArch()          // e.g. "amd64", "aarch64"
print javaVersion()     // e.g. "21.0.2"
print freeMemory()      // JVM free heap bytes
print totalMemory()     // JVM total heap bytes
gc()                    // suggest GC (hint only)
```

---

## 19. Error Handling

Larv errors are not catchable inside scripts in the current version. When an error occurs, the interpreter prints a formatted message and exits with code 1.

### Error format

```
[KIND ERROR] line N, col C: message
```

For runtime errors that do not have column information:

```
[RUNTIME ERROR] line N: message
```

### Error kinds

| Kind      | Description                                       |
|-----------|---------------------------------------------------|
| `LEXER`   | Invalid character or unterminated string          |
| `PARSER`  | Unexpected token, missing `{`, `}`, `;`, etc.    |
| `RUNTIME` | Type mismatch, undefined variable, index OOB, etc.|
| `FFI`     | Java class not found, method not found, etc.      |

### Common runtime errors

| Message                                       | Cause                                      |
|-----------------------------------------------|--------------------------------------------|
| `Undefined variable 'x'`                      | Using a name before declaring it           |
| `pop() called on an empty array`              | Calling `.pop()` on `[]`                   |
| `remove() index N is out of bounds`           | Index beyond array length                  |
| `len() expects an array`                      | Passing a non-array to `len()`             |
| `strSlice(): index out of bounds`             | `from`/`to` outside string length          |
| `Java class not found: 'com.X'`              | Wrong fully-qualified name in `include`    |

---

## 20. Grammar Summary

```
program       → statement* EOF

statement     → varDecl | constDecl | printStmt | ifStmt
              | whileStmt | forStmt | foreachStmt
              | funcDecl | returnStmt | classDecl
              | importStmt | includeStmt
              | breakStmt | continueStmt
              | exprStmt

varDecl       → "var" IDENTIFIER ("=" expression)?
constDecl     → "const" IDENTIFIER "=" expression
printStmt     → "print" expression
ifStmt        → "if" expression "{" block "}" ("else" "{" block "}")?
whileStmt     → "while" expression "{" block "}"
forStmt       → "for" forInit ";" expression ";" forIncr "{" block "}"
foreachStmt   → "for" IDENTIFIER ":" expression "{" block "}"
funcDecl      → "func" IDENTIFIER "(" params ")" "{" block "}"
returnStmt    → "return" expression
classDecl     → "class" IDENTIFIER "{" funcDecl* "}"
importStmt    → "import" (IDENTIFIER | STRING)
includeStmt   → "include" IDENTIFIER "from" STRING
              ("involve" "{" STRING ("," STRING)* "}")?
breakStmt     → "break"
continueStmt  → "continue"
exprStmt      → expression

forInit       → IDENTIFIER "=" expression | exprStmt
forIncr       → IDENTIFIER "++" | IDENTIFIER "--" | exprStmt
params        → (IDENTIFIER ("," IDENTIFIER)*)?
block         → statement* "}"

expression    → assignment
assignment    → (call ".")? IDENTIFIER "=" assignment
              | (call "[" expression "]") "=" assignment
              | equality
equality      → comparison (("==" | "!=") comparison)*
comparison    → addition (("<" | ">" | "<=" | ">=") addition)*
addition      → multiplication (("+" | "-") multiplication)*
multiplication→ unary (("*" | "/") unary)*
unary         → "-" unary | call
call          → primary (("(" args ")" | "." IDENTIFIER ("(" args ")")?
              | "[" expression "]"))*
primary       → NUMBER | STRING | "true" | "false" | "nil"
              | IDENTIFIER | "this" | "new" IDENTIFIER "(" args ")"
              | "[" args "]" | "(" expression ")"

args          → (expression ("," expression)*)?
```

---

*Larv 1.0.0-beta — created by Abd Allah Al Habbash*