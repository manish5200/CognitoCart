ALTER TABLE orders
    ADD CONSTRAINT chk_orders_return_reason
        CHECK (return_reason IS NULL OR return_reason IN (
                                                          'DEFECTIVE',
                                                          'WRONG_ITEM',
                                                          'DAMAGED_IN_TRANSIT',
                                                          'CHANGED_MIND',
                                                          'NOT_AS_DESCRIBED',
                                                          'SIZE_MISMATCH'
            ));
