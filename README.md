# doSonarAPI YAML Grammar builder

doSonarAPI YAML Grammar builder is a Sonar SSLR extension to build grammars based on JSON/YAML documents. 

doSonarAPI YAML Grammar builder is the spiritual successor of [Sonar YAML Grammar builder](https://github.com/societe-generale/sslr-yaml-parser), carrying on from the point where it left off with support of Apiaddicts community.

## How to build

* Make sure you have at least a JDK 1.8
* Install [Maven]()
* Launch Maven install

## License

Copyright 2021 Apiaddicts.

Licensed under the [GNU Lesser General Public License, Version 3.0](http://www.gnu.org/licenses/lgpl.txt)

## How to use

This plugin is designed to be used in conjunction with the Sonar SSLR Framework. It extends it with new grammar primitives
to describe arrays, objects and their properties (mandatory or optional).

### Building your grammar

An object is described as a standard Sonar SSLR rule. The most widely adopted pattern is to create an enum for all your
grammar's rules, and to provide a `create()` method to populate the builder.

```java
public enum OpenApi3Grammar implements GrammarRuleKey {
    ROOT,
    INFO,
    CONTACT,
    LICENSE,
    CALLBACK;

    public static YamlGrammarBuilder create() {
        // build your grammar here
    }
}
```

The `YamlGrammarBuilder` class is very similar to the `LexerfulGrammarBuilder` provided by the SSLR framework. It
provides helper methods to describe your grammar, such as rule definitions, one-of choices, sequences, etc. As with the
default SSLR toolkit, you must define a root rule for your document.

```java
    public static YamlGrammarBuilder create() {
        YamlGrammarBuilder b = new YamlGrammarBuilder();
        b.setRootRule(ROOT);
    
        b.rule(ROOT).is(b.object(
           // properties definitions
        );
        
        return b;
    }
```

#### Describing objects

To describe objects, the YAML grammar builder provides new primitives to express which properties are expected in an
object. Most properties are optional in YAML schemas, so by default the `property()` methods defines an optional
property - the parsing of the document will not fail if the property is missing. To define mandatory properties and
trigger a parsing error if the field is missing, use `mandatoryProperty()` instead.

```java
    b.rule(EXTERNAL_DOC).is(b.object(
        b.property("description", b.string()),
        b.mandatoryProperty("url", b.string())
    ));
```

To define properties that match a certain pattern, use the `patternProperty` method. Pattern properties are always optional.
**Important note: the order of pattern definition matters**. Define the most restrictive rule first to avoid starving
the more open rule.

```java
    // GOOD EXAMPLE: the most restrictive rule is defined first
    b.rule(EXTERNAL_DOC).is(b.object(
        b.patternProperty("^x-.*", b.anything())),
        b.patternProperty(".*", PATH)
    );
    // BAD EXAMPLE: the second rule will never be matched
    b.rule(EXTERNAL_DOC).is(b.object(
        b.patternProperty(".*", PATH),
        b.patternProperty("^x-.*", b.anything())
    );
```

You can also define arrays, as rule targets or as properties:
```java
    b.rule(AN_ARRAY).is(b.array(b.string()));
    b.rule(ROOT).is(b.object(
        // ...
        b.property("servers", b.array(SERVER)),
        // ...
    );
```

#### Describing property values

The YAML Grammar builder comes with matchers for the standard YAML scalar types: `string()`, `integer()`,
`float()`, `boolean()` and `null()`. You can use them in rules definitions, as demonstrated
in the examples above.

As in the standard SSLR toolkit, you can also define other rules as targets of the properties.

```java
        b.rule(INFO).is(b.object(
                b.mandatoryProperty("title", b.string()),
                b.property("contact", CONTACT),
                b.property("license", LICENSE),
                b.mandatoryProperty("version", b.string()),
                b.patternProperty("^x-.*", b.anything())
        ));

        b.rule(CONTACT).is(b.object(
                b.property("name", b.string()),
                b.property("url", b.string()),
                b.property("email", b.string()),
                b.patternProperty("^x-.*", b.anything())
        ));
```

The YAML Grammar builder also defines basic rules for generic JSON objects:
* `anything()` to match any valid JSON/YAML payload (object, array, scalar)
* `scalar()` to match any scalar
* `anyObject()` to match any valid JSON/YAML object
* `anyArray()` to match any valid JSON/YAML array
* `string()` to match a string value
* `bool()` and `bool(true|false)` to match any or a specific boolean value
* `floating()` and `integer()` to match a floating point value or integer value, respectively.

The YAML grammar builder allows you to define restrictions to the values that textual and boolean properties can take:

```java
    b.mandatoryProperty("openapi", b.firstOf("3.0.0", "3.0.1")),
    b.property("onlyFalse", b.bool(false))
```

> **Note**: the `object()` method only accepts property definitions. You cannot use basic composers on property definitions,
> only on property values.

### Parsing documents with the YAML scanner

The SSLR toolkit allows one to define lexers with a series of channels. However, the YAML grammar takes a different
approach and builds on a 2-pass parsing:
* the first pass parses the document as a regular YAML document, and generate an SSLR AST;
* the second pass inspects the AST for conformity with your grammar, and generates a new syntax tree.

The plugin provides an integrated scanner that performs these 2 passes.
To parse a document, compose the parser with your grammar definition, as with any SSLR grammar/lexer combination:

```java
    YamlParser parser = new YamlParser(Charset.forName("UTF-8"), OpenApi3Grammar.create());
    JsonNode node = parser.parse(new File("myYamlFile.yaml"));
```

### Inspecting your documents

To facilitate the parsing of documents, the `JsonNode` class offers tools to navigate between in properties of a document's
abstract syntax tree, get back to the property key, and find elements of an array. These tools are strongly inspired by
[Jackson]()'s JsonNode class.

* Selecting a property node: `JsonNode propertyNode = parent.at("/my/fourth/level/property")`
* Getting the value from a property: `JsonNode valueNode = propertyNode.value()`
* Getting the key from a valueNode: `JsonNode keyNode = valueNode.key()` or `parent.at("/my/property").key()`
* Getting elements of an array: `List<JsonNode> elements = parent.at("/my/array/property").elements()`
* Getting all properties of an object: `Collection<JsonNode> properties = parent.at("/my/array/property").properties()`

To extract the actual value of a scalar node, use `JsonNode.stringValue()`, `JsonNode.intValue()`, `JsonNode.booleanValue()`
or `JsonNode.intValue()` as needed.

As with the regular SSLR toolkit, you can access the node's token with `JsonNode.getToken()` to inspect its document
properties, such as `getLine()`, `getColumn()` or `getOriginalValue()`.

## Performing a new release

Validate that all is correct:

`mvn -Drelease package`

Deploy:

`mvn -Drelease deploy`
