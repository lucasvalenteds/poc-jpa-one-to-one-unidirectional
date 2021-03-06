CREATE SEQUENCE DOCUMENT_ID_SEQUENCE START WITH 1;

CREATE TABLE DOCUMENT
(
    DOCUMENT_ID         BIGINT,
    DOCUMENT_CODE       VARCHAR,
    DOCUMENT_EXPIRES_AT TIMESTAMP WITH TIME ZONE,

    PRIMARY KEY (DOCUMENT_ID)
);
