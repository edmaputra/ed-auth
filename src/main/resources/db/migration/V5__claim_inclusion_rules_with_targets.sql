CREATE TABLE IF NOT EXISTS claim_inclusion_rules (
    attribute_key varchar(120) NOT NULL,
    targets varchar(300) NOT NULL,
    PRIMARY KEY (attribute_key)
);