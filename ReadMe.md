# Larv

A statically-compiled, dynamically-typed programming language that runs on the JVM. Larv compiles directly to JVM bytecode via ASM, supports a full interpreter for scripting mode, and provides a rich standard library out of the box.

## Version
1.1.0-beta

### Write once, run everywhere

```larv
import "string"

class Greeter {
    func init(name) {
        this.name = name
    }

    func greet() {
        print("Hello, " + strUpper(this.name) + "!")
    }
}

var g = new Greeter("world")
g.greet()
```

---

## Table of Contents

- [Installation](#installation)
- [Running Larv](#running-larv)
- [Language Basics](#language-basics)
  - [Variables and Constants](#variables-and-constants)
  - [Data Types](#data-types)
  - [Operators](#operators)
  - [String Concatenation](#string-concatenation)
  - [Nil](#nil)
- [Type Annotations](#type-annotations)
- [Control Flow](#control-flow)
  - [If / Else](#if--else)
  - [Ternary Expression](#ternary-expression)
  - [While](#while)
  - [Traditional For](#traditional-for)
  - [For-in (Foreach)](#for-in-foreach)
  - [Map Iteration](#map-iteration)
  - [Switch](#switch)
  - [Break and Continue](#break-and-continue)
- [Functions](#functions)
  - [Function Modifiers](#function-modifiers)
  - [Return Types](#return-types)
- [Classes](#classes)
  - [Fields and Methods](#fields-and-methods)
  - [Constructor](#constructor)
  - [this](#this)
  - [Inheritance](#inheritance)
  - [Method Modifiers](#method-modifiers)
  - [Property Accessors](#property-accessors)
- [Modules](#modules)
- [Enums](#enums)
- [Concurrency](#concurrency)
  - [Atomic Variables](#atomic-variables)
  - [Volatile Variables](#volatile-variables)
  - [Synchronized Functions](#synchronized-functions)
- [Defer](#defer)
- [Error Handling](#error-handling)
- [Imports](#imports)
  - [Stdlib Imports](#stdlib-imports)
  - [File Imports](#file-imports)
- [Java Interop (FFI)](#java-interop-ffi)
- [Standard Library](#standard-library)
  - [math](#math)
  - [string](#string)
  - [list](#list)
  - [map](#map)
  - [io](#io)
  - [path](#path)
  - [json](#json)
  - [date](#date)
  - [http](#http)
  - [regex](#regex)
  - [base64 / encode](#base64--encode)
  - [properties](#properties)
  - [system](#system)
  - [convert](#convert)
  - [jdbc](#jdbc)
  - [socket](#socket)
  - [serverSocket](#serversocket)
  - [thread](#thread)
- [Compiler](#compiler)
  - [Compile a File](#compile-a-file)
  - [Compiler Options](#compiler-options)
  - [Debug Mode](#debug-mode)
  - [How It Works](#how-it-works)
- [Project Structure](#project-structure)

---

## Installation

Larv requires **Java 21** or later.

Build from source with Maven:

```bash
git clone https://github.com/habbashx/larv
cd larv
mvn package
```

The resulting JAR is at `target/larv.jar`.

---

## Running Larv

**Interpreter (scripting) mode — run a `.larv` file directly:**
```bash
java -jar larv.jar run Main.larv
```

**Compiler mode — compile to `.class` files then run:**
```bash
java -jar larv.jar compile Main.larv --run
```

**Compile only (write `.class` files to disk):**
```bash
java -jar larv.jar compile Main.larv --out ./out
```

---

## Language Basics

### Variables and Constants

```larv
var name = "Alice"
var age  = 30
var active = true

const PI = 3.14159
const APP_NAME = "Larv"
```

Variables declared with `var` are mutable. Constants declared with `const` cannot be reassigned.

### Data Types

| Type    | Example                        |
|---------|--------------------------------|
| Number  | `42`, `3.14`, `-7`             |
| String  | `"hello"`, `'world'`           |
| Boolean | `true`, `false`                |
| Nil     | `nil`                          |
| Object  | `new MyClass()`                |

Raw (multi-line) strings use backticks:
```larv
var sql = `SELECT *
           FROM users
           WHERE active = true`
```

### Operators

| Category   | Operators                              |
|------------|----------------------------------------|
| Arithmetic | `+`  `-`  `*`  `/`                     |
| Comparison | `==`  `!=`  `<`  `>`  `<=`  `>=`      |
| Logical    | `&&`  `\|\|`  `!`                      |
| Assignment | `=`  `+=`  `-=`  `*=`  `/=`           |
| Increment  | `++`  `--`                             |

### String Concatenation

The `+` operator concatenates when either operand is a string:

```larv
var msg = "Hello, " + "world"
var info = "Age: " + 30        // "Age: 30"
```

### Nil

`nil` represents the absence of a value. Variables declared without an initial value are `nil`.

```larv
var x         // x is nil
var y = nil
```

---

## Type Annotations

Variables, constants, and function parameters can carry optional type annotations after a `:`. The type is checked at compile time — a mismatch is a compile error.

```larv
var score: int = 0
var name: string = "Alice"
const MAX: int = 100

func greet(name: string) -> string {
    return "Hello, " + name
}
```

Supported types: `int`, `double`, `float`, `long`, `string`, `bool`, `any` (the default — no checking).

---

## Control Flow

### If / Else

```larv
if age >= 18 {
    print("adult")
} else if age >= 13 {
    print("teen")
} else {
    print("child")
}
```

### Ternary Expression

```larv
var label = age >= 18 ? "adult", "minor"
```

### While

```larv
var i = 0
while i < 10 {
    print(i)
    i++
}
```

### Traditional For

```larv
for i = 0; i < 5; i++ {
    print(i)
}
```

### For-in (Foreach)

Iterate over any list:

```larv
var fruits = ["apple", "banana", "cherry"]

for fruit in fruits {
    print(fruit)
}
```

### Map Iteration

Iterate over a map's key-value pairs using two loop variables separated by a comma:

```larv
var scores = Map("Alice", 95, "Bob", 87)

for name, score in scores {
    print(name + " scored " + score)
}
```

The first variable receives the key and the second receives the value on every iteration.

### Switch

Multiple values on a single `case` act as OR conditions. No fall-through between cases.

```larv
switch status {
    case "active", "enabled" : {
        print("running")
    }
    case "paused" : {
        print("paused")
    }
    default : {
        print("unknown status")
    }
}
```

### Break and Continue

```larv
for i = 0; i < 10; i++ {
    if i == 3 { continue }
    if i == 7 { break }
    print(i)
}
```

---

## Functions

```larv
func add(a, b) {
    return a + b
}

var result = add(3, 4)   // 7
```

Functions are first-class values — they can be stored in variables and passed as arguments:

```larv
func apply(f, x) {
    return f(x)
}

func double(n) {
    return n * 2
}

print(apply(double, 5))   // 10
```

Recursive functions work as expected:

```larv
func fib(n) {
    if n <= 1 { return n }
    return fib(n - 1) + fib(n - 2)
}
```

### Function Modifiers

Three modifiers can appear before `func`:

**`sync`** — the function body executes under a per-function lock. Only one thread can be inside it at a time:

```larv
sync func increment() {
    this.count += 1
}
```

**`core`** — the function cannot be overridden by subclasses (compiles to a `final` JVM method):

```larv
core func validate(input) {
    return input != nil
}
```

**`override`** — explicitly marks the function as overriding a parent class method:

```larv
override func speak() {
    print("Woof!")
}
```

Modifiers can be combined: `sync override func save() { ... }`

### Return Types

Declare the return type after `->`. The compiler verifies that the returned value matches:

```larv
func square(n: int) -> int {
    return n * n
}

func fullName(first: string, last: string) -> string {
    return first + " " + last
}
```

---

## Classes

### Fields and Methods

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

var dog = new Animal("Rex", "woof")
dog.speak()
```

### Constructor

The `init` method is the constructor. It is called automatically when the object is created with `new`. Any number of parameters are supported.

### this

Inside any method, `this` refers to the current object instance. Use it to read and write instance fields.

```larv
class Counter {
    func init() {
        this.count = 0
    }

    func increment() {
        this.count += 1
    }

    func value() {
        return this.count
    }
}
```

### Inheritance

A class can extend another class using `:` after the class name. The child class inherits all methods from the parent and can add or override them:

```larv
class Animal {
    func init(name) {
        this.name = name
    }

    func speak() {
        print(this.name + " makes a sound")
    }
}

class Dog : Animal {
    func init(name) {
        this.name = name
    }

    override func speak() {
        print(this.name + " barks!")
    }
}

var d = new Dog("Rex")
d.speak()   // Rex barks!
```

### Method Modifiers

The same `sync`, `core`, and `override` modifiers that apply to top-level functions apply inside class bodies with the same semantics — `sync` synchronizes on the receiver instance, `core` makes the method non-overridable, `override` declares an intentional override.

### Property Accessors

Variables can be declared with `get` and `set` accessors:

```larv
var score = 0 : get, set
const VERSION = "1.0" : get
```

---

## Modules

Modules are singleton namespaces — unlike classes they are not instantiated with `new`.

```larv
module Config {
    var host = "localhost"
    var port = 8080

    func url() {
        return "http://" + host + ":" + port
    }
}

print(Config.url())
```

---

## Enums

```larv
enum Direction { NORTH, SOUTH, EAST, WEST }
enum Status    { ACTIVE, INACTIVE, PENDING }

var dir = Direction.NORTH
print(dir)   // NORTH
```

---

## Concurrency

### Atomic Variables

`atomic<type>` declares a thread-safe variable backed by a Java `AtomicReference`. Reads and writes are guaranteed to be atomic across threads:

```larv
atomic<int> counter = 0
atomic<bool> running = true
atomic<string> status
```

### Volatile Variables

`volatile var` declares a variable whose value is always read from and written to main memory — no thread-local caching. Use this for simple flags shared between threads when you don't need the full atomic read-modify-write guarantee:

```larv
volatile var active = true

// another thread can set active = false and this thread will see it immediately
while active {
    doWork()
}
```

`volatile` is only allowed on class fields.

### Synchronized Functions

Mark a function with `sync` to ensure exclusive access. On top-level functions a per-function lock is used; inside classes the receiver object is the monitor:

```larv
class BankAccount {
    func init(balance) {
        this.balance = balance
    }

    sync func deposit(amount) {
        this.balance += amount
    }

    sync func withdraw(amount) {
        this.balance -= amount
    }
}
```

---

## Defer

`defer` schedules an expression to run when the enclosing scope exits, in LIFO order relative to other defers in the same scope. It mirrors Go's `defer` and is the idiomatic way to close resources:

```larv
var file = openFile("data.txt")
defer file.close()

// use file ...
// file.close() runs automatically here, even if an error is thrown
```

Multiple defers in the same scope execute in reverse order:

```larv
defer print("third")
defer print("second")
defer print("first")
// prints: first, second, third
```

---

## Error Handling

### try / catch / finally

```larv
try {
    var result = riskyOperation()
} catch (e) {
    print("caught: " + e)
} finally {
    print("always runs")
}
```

`catch` and `finally` are both optional, but at least one must be present. The caught variable `e` holds the error message string or the thrown value.

### throw

```larv
func divide(a, b) {
    if b == 0 {
        throw "division by zero"
    }
    return a / b
}

try {
    divide(10, 0)
} catch (e) {
    print(e)   // "division by zero"
}
```

Any value can be thrown — strings, numbers, objects.

---

## Imports

### Stdlib Imports

```larv
import "math"
import "string"
import "io"
import "json"
```

After importing, the library's functions are available directly:

```larv
import "math"

print(sqrt(16))    // 4
print(pow(2, 10))  // 1024
```

### File Imports

Import classes and functions from other `.larv` files using a dotted path. The path is resolved relative to the project root.

```larv
// main.larv
import "services.UserService"

var svc = new UserService("db")
svc.createUser("Alice")
```

```larv
// services/UserService.larv
import "jdbc"

class UserService {
    func init(alias) {
        this.alias = alias
    }

    func createUser(name) {
        dbInsert(this.alias, "users", {"name": name})
    }
}
```

File imports are **circular-import safe** — importing the same file twice is a no-op. Only top-level class and function declarations are imported; top-level executable statements in the imported file do not run.

---

## Java Interop (FFI)

Bind any Java class and call its methods directly from Larv using `include ... from`.

**Static binding** — access static methods:

```larv
include Math from "java.lang.Math"

print(Math.sqrt(144))    // 12.0
print(Math.PI)
```

**Instance binding** — create a Java object using `involve`:

```larv
include scanner from "java.util.Scanner" involve {
    "java.lang.System.in"
}

var line = scanner.nextLine()
print("You typed: " + line)
```

The `involve` block accepts the constructor arguments as strings. Larv resolves:
- Static fields like `"java.lang.System.in"` to their actual values
- Numeric strings like `"1024"` to the appropriate primitive type
- Class constructor strings like `"some.Class(arg)"` to new instances

---

## Standard Library

All stdlib functions are available after importing the relevant library name.

---

### math

```larv
import "math"

sqrt(x)           // square root
pow(base, exp)    // exponentiation
abs(x)            // absolute value
floor(x)          // round down
ceil(x)           // round up
round(x)          // round to nearest
min(a, b)         // smaller of two
max(a, b)         // larger of two
log(x)            // natural log
log10(x)          // base-10 log
sin(x)            // trigonometric sine
cos(x)            // cosine
tan(x)            // tangent
asin(x)           // arc sine
acos(x)           // arc cosine
atan(x)           // arc tangent
atan2(y, x)       // two-argument arc tangent
toRadians(deg)    // degrees → radians
toDegrees(rad)    // radians → degrees
random()          // random float in [0, 1)
randomInt(lo, hi) // random integer in [lo, hi]
clamp(n, lo, hi)  // clamp n between lo and hi
sign(x)           // -1, 0, or 1
pi()              // π
e()               // Euler's number
isNaN(x)          // true if NaN
isInfinite(x)     // true if infinite
toInt(x)          // truncate to integer
```

---

### string

```larv
import "string"

strLen(s)                   // length
strUpper(s)                 // uppercase
strLower(s)                 // lowercase
strTrim(s)                  // strip whitespace both sides
strTrimLeft(s)              // strip leading whitespace
strTrimRight(s)             // strip trailing whitespace
strContains(s, sub)         // true if sub is in s
strStartsWith(s, prefix)    // true if starts with prefix
strEndsWith(s, suffix)      // true if ends with suffix
strIndexOf(s, sub)          // first index of sub, or -1
strSlice(s, from, to)       // substring [from, to)
strReplace(s, old, new)     // replace first occurrence
strReplaceAll(s, old, new)  // replace all occurrences
strSplit(s, sep)             // split into list
strJoin(list, glue)          // join list into string
strRepeat(s, n)              // repeat s n times
strReverse(s)               // reverse the string
strCharAt(s, i)             // character at index i
strToNumber(s)              // parse string to number
strFromNumber(n)            // number to string
strIsEmpty(s)               // true if blank
strPadLeft(s, n, ch)        // left-pad to length n
strPadRight(s, n, ch)       // right-pad to length n
strChars(s)                 // list of individual characters
```

---

### list

```larv
import "list"

listNew()               // create empty list
listSize(l)             // number of elements
listIsEmpty(l)          // true if empty
listAdd(l, val)         // append value
listAddAt(l, i, val)    // insert at index
listGet(l, i)           // get element at index
listSet(l, i, val)      // set element at index
listRemove(l, i)        // remove element at index
listContains(l, val)    // true if value exists
listIndexOf(l, val)     // first index of value
listSlice(l, from, to)  // sublist [from, to)
listReverse(l)          // reversed copy
listSort(l)             // sorted copy (numeric)
listConcat(a, b)        // join two lists
listFlat(l)             // flatten one level of nesting
listUnique(l)           // remove duplicates, preserve order
listFill(n, val)        // list of n copies of val
listClear(l)            // remove all elements
listFirst(l)            // first element or nil
listLast(l)             // last element or nil
listPop(l)              // remove and return last element
listShuffle(l)          // shuffled copy
```

---

### map

```larv
import "map"

mapNew()                  // create empty map
mapSize(m)                // number of entries
mapIsEmpty(m)             // true if empty
mapSet(m, key, val)       // set key
mapGet(m, key)            // get value for key
mapHas(m, key)            // true if key exists
mapRemove(m, key)         // delete key
mapClear(m)               // remove all entries
mapKeys(m)                // list of keys
mapValues(m)              // list of values
mapContainsValue(m, val)  // true if value exists
mapMerge(a, b)            // merge b into a, return new map
mapToList(m)              // list of {key, value} maps
```

---

### io

```larv
import "io"

readFile(path)              // read file to string
writeFile(path, content)    // write string to file
appendFile(path, content)   // append string to file
readLines(path)             // list of lines
readBytes(path)             // list of byte values
writeBytes(path, bytes)     // write byte list to file
deleteFile(path)            // delete file, true if deleted
fileExists(path)            // true if path exists
isDir(path)                 // true if directory
listDir(path)               // list immediate children
makeDir(path)               // create directory tree
copyFile(src, dst)          // copy file
moveFile(src, dst)          // move/rename file
fileSize(path)              // size in bytes
cwd()                       // current working directory
absPath(path)               // absolute path string
```

---

### path

```larv
import "path"

// Construction
pathJoin(a, b, ...)         // join segments: pathJoin("/usr", "local", "bin")
pathAbs(p)                  // absolute path
pathNormalize(p)            // remove . and .. without filesystem access
pathRelative(base, target)  // express target relative to base

// Decomposition
pathParent(p)               // parent directory
pathFileName(p)             // last component with extension
pathStem(p)                 // last component without extension
pathExt(p)                  // extension including dot, e.g. ".txt"
pathParts(p)                // list of all path components
pathDepth(p)                // number of components

// Predicates
pathExists(p)               // true if path exists
pathIsFile(p)               // true if regular file
pathIsDir(p)                // true if directory
pathIsAbsolute(p)           // true if absolute
pathIsRelative(p)           // true if relative
pathIsHidden(p)             // true if hidden
pathIsSymlink(p)            // true if symbolic link
pathIsEmpty(p)              // true if empty dir or zero-byte file
pathStartsWith(p, prefix)   // path prefix check
pathEndsWith(p, suffix)     // path suffix check
pathSame(a, b)              // true if same filesystem entry

// Manipulation (no I/O)
pathWithExt(p, ext)         // replace extension
pathWithName(p, name)       // replace filename
pathWithStem(p, stem)       // replace stem, keep extension
pathCommon(a, b)            // longest common ancestor path

// Metadata
pathSize(p)                 // size in bytes
pathCreated(p)              // creation time in ms since epoch
pathModified(p)             // last modified time in ms
pathStat(p)                 // map: {size, created, modified, isFile, isDir, isSymlink, isHidden}

// Directory listing
pathList(p)                 // immediate children
pathListAll(p)              // all descendants recursively
pathGlob(dir, pattern)      // entries matching glob e.g. "**/*.larv"
pathWalk(p)                 // list of stat maps for all entries

// Filesystem operations
pathMakeDir(p)              // create directory (parent must exist)
pathMakeDirs(p)             // create directory and all parents
pathDelete(p)               // delete file or empty directory
pathDeleteAll(p)            // recursively delete tree
pathCopy(src, dst)          // copy (overwrites)
pathMove(src, dst)          // move/rename (overwrites)
pathSymlink(link, target)   // create symbolic link
pathResolveSymlink(p)       // real path after resolving symlinks

// System paths
pathCwd()                   // current working directory
pathHome()                  // user home directory
pathTemp()                  // system temp directory
pathSep()                   // platform separator ("/" or "\")
```

---

### json

```larv
import "json"

jsonStringify(value)        // value → compact JSON string
jsonPretty(value)           // value → pretty-printed JSON string
jsonParse(str)              // JSON string → Larv value
jsonValid(str)              // true if str is valid JSON
jsonRead(path)              // read and parse a JSON file
jsonWrite(path, value)      // encode and write JSON to a file
```

---

### date

```larv
import "date"

dateNow()                   // current timestamp in ms
timeNow()                   // current time string
dateTimeNow()               // current date-time string
timestamp()                 // Unix timestamp in seconds
year()                      // current year
month()                     // current month number
day()                       // current day of month
hour()                      // current hour
minute()                    // current minute
second()                    // current second
dayOfWeek()                 // day name e.g. "Monday"
monthName()                 // month name e.g. "January"
dateFormat(ms, pattern)     // format timestamp with pattern
dateParse(str, pattern)     // parse date string to timestamp
dateAdd(ms, n, unit)        // add n units: "days","hours","minutes","seconds","weeks","months","years"
dateSub(ms, n, unit)        // subtract n units
dateDiff(a, b)              // difference in ms
isAfter(a, b)               // true if a > b
isBefore(a, b)              // true if a < b
```

---

### http

```larv
import "http"

httpGet(url)                     // GET request → {status, body}
httpPost(url, body)              // POST with plain body
httpPostJson(url, jsonStr)       // POST with Content-Type: application/json
httpPut(url, body)               // PUT request
httpDelete(url)                  // DELETE request
httpRequest(method, url, body)   // generic request with any method
```

All functions return a map with `status` (number) and `body` (string).

---

### regex

```larv
import "regex"

regexMatch(str, pattern)              // true if entire string matches
regexTest(str, pattern)               // true if pattern is found anywhere
regexFind(str, pattern)               // true if pattern is found
regexFindAll(str, pattern)            // list of all matches
regexGroups(str, pattern)             // list of capture groups from first match
regexGroup(str, pattern, n)           // nth capture group
regexReplace(str, pattern, repl)      // replace first match
regexReplaceAll(str, pattern, repl)   // replace all matches
regexSplit(str, pattern)              // split on pattern
```

---

### base64 / encode

```larv
import "base64"

base64Encode(str)   // encode string to Base64
base64Decode(str)   // decode Base64 to string
urlEncode(str)      // percent-encode a URL string
urlDecode(str)      // decode a percent-encoded string
hexEncode(str)      // encode string to hex
hexDecode(hex)      // decode hex to string
```

---

### properties

```larv
import "properties"

loadProp(file)       // load a .properties file into memory
getProp(key)         // get a value from the loaded file
getAllProps()         // map of all loaded properties
setProp(key, value)  // set a property in memory
saveProp(file)       // save current properties back to file
```

---

### system

```larv
import "system"

exit(code)          // exit with status code
getEnv(name)        // read environment variable
getArgs()           // command-line arguments as list
clock()             // current time in milliseconds
nanoTime()          // current time in nanoseconds
sleep(ms)           // pause for ms milliseconds
exec(command)       // run shell command → {exit, out, err}
sh(command)         // shorthand for exec
osName()            // OS name e.g. "Linux"
osArch()            // OS architecture e.g. "amd64"
freeMemory()        // JVM free memory in bytes
totalMemory()       // JVM total memory in bytes
gc()                // request garbage collection
```

---

### convert

```larv
import "convert"

toInt(x)          // truncate to integer
toNumber(x)       // parse to number
toString(x)       // convert to string
toBool(x)         // convert to boolean ("true","yes","1","on" → true)
toBytes(x)        // string → list of byte values
fromBytes(list)   // list of byte values → string
toBinary(n)       // number → binary string
fromBinary(s)     // binary string → number
toHex(n)          // number → hex string (255 → "ff")
fromHex(s)        // hex string → number
toOctal(n)        // number → octal string
fromOctal(s)      // octal string → number
typeOf(x)         // type name as string: "string", "number", "bool", "list", "map", "nil"
```

---

### jdbc

Connect to any JDBC-compatible database. The driver JAR must be on the classpath.

```larv
import "jdbc"

// Connect — any JDBC driver works
dbOpen("db", "org.postgresql.Driver", "jdbc:postgresql://localhost/mydb", "user", "pass")
dbOpen("sq", "org.sqlite.JDBC",       "jdbc:sqlite:data.db",              "",     "")

// Parameterised query — always use ? for values
var rows = dbQuery("db", "SELECT * FROM users WHERE role = ?", ["admin"])

// Execute DDL/DML
dbExec("db", "CREATE TABLE logs (id SERIAL, msg TEXT)", [])

// Convenience helpers (values always bound as parameters)
dbInsert("db", "users", {"name": "Alice", "age": 30})
dbUse("db")                           // set default connection
dbQueryOne("db", "SELECT * FROM users WHERE id = ?", [1])

// Transactions
dbBegin("db")
dbInsert("db", "orders", {"user_id": 1, "total": 99.99})
dbCommit("db")

// Metadata
var tables  = dbTables("db")
var columns = dbColumns("db", "users")

dbClose("db")
```

---

### socket

```larv
import "socket"

var s = connect("localhost", 8080)  // open TCP connection

send(s, "Hello server")            // send string
var reply = receive(s)             // receive string
var bytes = readBytes(s)           // receive as byte list
writeBytes(s, byteList)            // send byte list

setKeepAlive(s, true)              // socket options
setSoTimeout(s, 5000)
setTcpNoDelay(s, true)

getRemoteAddr(s)                   // remote address string
close(s)                           // close the connection
```

---

### serverSocket

```larv
import "serverSocket"

var server = bind(8080)            // listen on port
setSoTimeout(server, 30000)        // accept timeout in ms
var client = accept(server)        // block until a client connects
getPort(server)                    // port number
isClosed(server)                   // true if closed
close(server)                      // stop listening
```

---

### thread

```larv
import "thread"

var t = spawn(myFunc)              // run function in a new thread
threadJoin(t)                      // wait for thread to finish
threadIsAlive(t)                   // true if still running
threadId(t)                        // thread ID number
threadName(t)                      // thread name string
threadSleep(ms)                    // sleep current thread
threadCount()                      // active thread count
cpuCount()                         // available CPU cores

// Channels — communicate between threads
var ch = channelNew()
channelSend(ch, "message")
var msg = channelRecv(ch)          // blocks until a message arrives
channelClose(ch)
```

---

## Compiler

Larv has two execution modes: an **interpreter** for quick scripting and a **compiler** that produces real JVM `.class` files.

### Compile a File

```bash
# Compile and immediately run
java -jar larv.jar compile Main.larv --run

# Compile to ./out directory
java -jar larv.jar compile Main.larv --out ./out

# Run the compiled class
java -cp out Main
```

### Compiler Options

| Option           | Description                                                       |
|------------------|-------------------------------------------------------------------|
| `--out <dir>`    | Output directory for `.class` files (default: `out`)             |
| `--class <name>` | Override the generated main class name                            |
| `--dump`         | Print ASM bytecode disassembly to stdout                          |
| `--run`          | Compile then immediately execute                                  |
| `--debug`        | Enable debug mode (see below)                                     |

### Debug Mode

Pass `--debug` to enable the compiler's debug mode. It adds three layers of diagnostic output to `stderr` (normal compilation output is unaffected):

**Statement tracing** — every statement is logged before it is compiled, including its Larv type and source line:
```
[DEBUG] compileStatement  line=7  type=VarStatement
[DEBUG]   var  name=x  declared=int  inferred=int  resolvedType=int  slot=1
```

**Expression tracing** — every expression node, including the operator or value:
```
[DEBUG]   compileExpression  type=BinaryExpression  op=+
[DEBUG]     typeInfer  expr=VarExpression  → int
```

**Call routing** — all 8 routing paths in the call compiler are announced so you can see exactly which dispatch path was taken:
```
[DEBUG]     compileCall  caller=greet  args=1
[DEBUG]     → route: top-level static call  name=greet
[DEBUG]     → INVOKEVIRTUAL  class=User  method=getName
```

**Function entry/exit** — every function compilation is bracketed:
```
[DEBUG] begin compileFunction  name=main  params=0  returnType=void  line=1
[DEBUG] end compileFunction    name=main
```

**JVM line numbers** — `visitLineNumber` entries are emitted into the bytecode so that JVM stack traces point at the original Larv source line rather than `(Unknown Source)`.

**Exception capture** — when any exception is thrown during compilation, it is caught and logged at the point of failure (statement type, line, exception class, message, full Java stack trace) before being re-thrown. Fatal exceptions that escape the entire pipeline are logged again at the top level.

```bash
java -jar larv.jar compile Main.larv --debug --run 2>debug.log
```

### How It Works

The compiler pipeline:

1. **Lexer** — tokenises the `.larv` source into a flat token stream
2. **Parser** — builds an AST from the token stream
3. **File import resolution** — recursively parses imported `.larv` files and merges their class/function declarations into the AST before compilation (circular imports are skipped automatically)
4. **Compiler** — performs two passes over the AST:
  - *First pass* — collects all top-level function, class, module, and import declarations
  - *Second pass* — emits JVM bytecode via ASM
5. **Output** — writes `.class` files; optionally executes immediately via a custom `ClassLoader`

Each Larv class compiles to a JVM inner class (`Main$MyClass`). Top-level functions compile to private static methods on the main class. Stdlib calls are emitted as direct `invokestatic` instructions — no reflection, no `Object[]` allocation for calls with four or fewer arguments.

The compiled runtime (`LarvRuntime`) includes several performance optimisations: a monomorphic inline cache on the `invokeMethod` fast path, a `MutableCallSite` per `(class, opcode)` pair for call-site caching, a `ThreadLocal` `StringBuilder` pool for `stringify` and `join` paths, and type-tag dispatch that avoids `instanceof` chains on the most common value types.

---

## Project Structure

```
src/main/java/com/habbashx/larv/
├── lexer/
│   ├── Lexer.java              Token stream producer
│   ├── Token.java              Token record
│   └── TokenType.java          All token categories
├── parser/
│   ├── StatementParser.java    Statement grammar rules
│   ├── ExpressionParser.java   Expression grammar + precedence
│   ├── ArgumentParser.java     Parameter / argument list parsing
│   ├── Precedence.java         Operator precedence table
│   └── ast/
│       ├── statement/          One record per statement node
│       └── expression/         One record per expression node
├── runtime/
│   ├── Interpreter.java        Entry point for interpreted mode
│   ├── StatementExecutor.java  Executes each statement type
│   ├── ExpressionEvaluator.java Evaluates each expression type
│   ├── ExecutionContext.java    Scope, class registry, function registry
│   ├── Environment.java        Variable / constant bindings
│   ├── BinaryOperator.java     All binary operation logic
│   ├── FunctionInvoker.java    Function call mechanics and sync support
│   ├── LoopExecutor.java       Loop execution helpers
│   ├── TruthinessEvaluator.java Truthiness rules
│   ├── LarvObject.java         Runtime class instance
│   ├── call/
│   │   ├── LarvCallable.java   Callable interface
│   │   ├── NativeFunction.java Stdlib function wrapper
│   │   └── UserFunction.java   User-defined function wrapper
│   ├── ffi/
│   │   └── JavaClassRegistry.java  Java class binding and method dispatch
│   ├── importer/
│   │   └── LarvFileImporter.java   Multi-file import resolution
│   ├── registry/
│   │   └── NativeRegistry.java     Native function registry
│   └── stdlib/                 Interpreter stdlib (Native* classes)
├── compiler/
│   ├── AbstractLarvCompiler.java   Shared state, debug helpers, utilities
│   ├── TypeInferenceCompiler.java  Static type inference (no bytecode)
│   ├── CallCompiler.java           Call routing and dispatch
│   ├── ExpressionCompiler.java     Expression → bytecode
│   ├── StatementCompiler.java      Statement → bytecode
│   ├── ClassCompiler.java          Class / method compilation
│   ├── LarvCompiler.java           Top-level compiler orchestration
│   ├── LarvCompilerMain.java       CLI entry point for compiled mode
│   ├── CompiledClass.java          Name + bytecode pair
│   ├── classloader/
│   │   └── LarvClassLoader.java    In-process class loader for --run
│   ├── exception/
│   │   └── CompileException.java   Compile-time error
│   ├── runtime/
│   │   ├── LarvRuntime.java        Compiled-mode runtime helpers
│   │   ├── LarvMethods.java        Method ID constants
│   │   ├── LarvObject.java         Compiled-mode object representation
│   │   ├── LarvRuntimeException.java
│   │   └── LarvTypeMismatchException.java
│   ├── stdlib/
│   │   ├── LarvStdlib.java         Interface all stdlib libs implement
│   │   ├── LarvStdlibLoader.java   Auto-builds registry from all libs
│   │   └── libs/                   One file per stdlib library
│   └── util/
│       ├── LarvCompilerUtils.java      Bytecode utility helpers
│       ├── LarvCompilerTypeChecker.java Pre-compilation type checker
│       └── LocalVarTable.java          Local variable slot management
├── error/
│   ├── LarvError.java          Runtime error with kind and line number
│   └── ErrorReporter.java      Rich coloured diagnostics with source snippets
└── signal/
    ├── BreakSignal.java         break  control flow
    ├── ContinueSignal.java      continue control flow
    ├── ReturnSignal.java        return value carrier
    └── ThrowSignal.java         throw value carrier
```