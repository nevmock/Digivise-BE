package org.nevmock.digivise.domain.model.mongo.performance;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ModelData {

    private Long id;
    private String name;
    private int status;
    private String display_tag_label;
    private int add_to_cart_units;
    private int add_to_cart_buyers;
    private double placed_sales;
    private int placed_units;
    private int placed_buyers;
    private double paid_sales;
    private int paid_units;
    private int paid_buyers;
    private double confirmed_sales;
    private int confirmed_units;
    private int confirmed_buyers;
    private double paid_sales_per_buyers;
    private double placed_to_paid_buyers_rate;
    private double placed_to_confirmed_buyers_rate;
    private double repeat_placed_order_rate;
    private int average_days_to_repeat_placed_order;
    private double repeat_paid_order_rate;
    private int average_days_to_repeat_paid_order;
    private double repeat_confirmed_order_rate;
    private int average_days_to_repeat_confirmed_order;
}
