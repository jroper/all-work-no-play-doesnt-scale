# Feedback schema

# --- !Ups

CREATE TABLE Feedback (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    username varchar(255) NOT NULL,
    feedback varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

# --- !Downs

DROP TABLE Feedback;