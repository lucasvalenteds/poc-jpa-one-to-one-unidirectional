# POC: JPA One-to-One Unidirectional

It demonstrates how to use JPA to implement a one-to-one relationship.

The goal is to be able to persist information about people, passports and links between them. Every person must have one
or none passport registered, and we want to make the references consistent.

# How to run

| Description | Command          |
|:------------|:-----------------|
| Run tests   | `./gradlew test` |

## Preview

Entity Relationship Model:

```mermaid
classDiagram
direction BT
class Passport {
    Long  id
    String  code
    Instant  expiresAt
}
class Person {
    Long  id
    String  firstName
    String  lastName
}

Person "0..1" --> "0..1" Passport
```