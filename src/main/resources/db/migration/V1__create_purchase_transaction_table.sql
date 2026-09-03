CREATE TABLE purchase_transaction (
    id UUID PRIMARY KEY,
    description VARCHAR(50) NOT NULL,
    transaction_date DATE NOT NULL,
    purchase_amount NUMERIC(10, 2) NOT NULL,

    CONSTRAINT chk_purchase_amount_positive
        CHECK (purchase_amount > 0)
);