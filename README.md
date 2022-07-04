# POC: JPA One-to-One Unidirectional

It demonstrates how to use JPA to implement a one-to-one relationship.

The goal is to be able to persist information about people, documents and links between them. Every person must have one
or none document registered, and we want to make the references consistent.

## How to run

| Description | Command          |
|:------------|:-----------------|
| Run tests   | `./gradlew test` |

## Preview

Entity Relationship Model:

```mermaid
classDiagram
direction LR

class Document {
    Long  id
    String  code
    Instant  expiresAt
}
class Person {
    Long  id
    String  name
}

Person "0..1" --> "0..1" Document
```

Database schema:

```mermaid
classDiagram
direction LR

class document {
   varchar document_code
   timestamp with time zone document_expires_at
   bigint document_id
}

class person {
   varchar person_name
   bigint document_id
   bigint person_id
}

person  -->  document : document_id
```
