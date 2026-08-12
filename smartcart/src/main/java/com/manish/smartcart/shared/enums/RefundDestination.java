package com.manish.smartcart.shared.enums;

public enum RefundDestination {
    ORIGINAL,  // Bank refund (Razorpay) + original wallet amount back to wallet
    WALLET     // Everything (Razorpay amount + Wallet amount) goes into the Wallet instantly
}
