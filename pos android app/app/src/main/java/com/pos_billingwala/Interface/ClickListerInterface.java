package com.pos_billingwala.Interface;

import com.pos_billingwala.Model.ProductResponse;

public interface ClickListerInterface {

    void categoryClicked(String categoryId);

    void productClicked(ProductResponse productResponse);

    /** Decrease quantity for a product already in the cart (product grid − button). */
    default void productQuantityDecreased(ProductResponse productResponse) {
    }

}
