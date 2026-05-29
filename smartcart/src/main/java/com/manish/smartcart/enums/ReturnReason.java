package com.manish.smartcart.enums;

public enum ReturnReason {

    DEFECTIVE(true),
    WRONG_ITEM(true),
    DAMAGED_IN_TRANSIT(true),
    CHANGED_MIND(false),
    NOT_AS_DESCRIBED(false),
    SIZE_MISMATCH(false),
    OTHER(false);

    private final boolean imageProofRequired;
    ReturnReason(boolean imageProofRequired) {
        this.imageProofRequired = imageProofRequired;
    }
    /**
     * Called in OrderService instead of magic string comparisons.
     * DEFECTIVE, WRONG_ITEM, DAMAGED_IN_TRANSIT → true (must upload images)
     * CHANGED_MIND, NOT_AS_DESCRIBED, SIZE_MISMATCH → false
     */
    public boolean requiresImageProof() {
        return imageProofRequired;
    }
}
