package org.nevmock.digivise.domain.model.mongo.performance;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    private Long id;
    private String name;
    private String image;
    private int status;
    private String display_tag_label;
    private int uv;
    private int pv;
    private int likes;
    private int bounce_visitors;
    private double bounce_rate;
    private int search_clicks;
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
    private double placed_buyers_to_confirmed_buyers_rate;
    private int placed_order_per_buyers;
    private int paid_order_per_buyers;
    private int confirmed_order_per_buyers;
    private double uv_to_add_to_cart_rate;
    private double uv_to_placed_buyers_rate;
    private double uv_to_paid_buyers_rate;
    private double uv_to_confirmed_buyers_rate;
    private double repeat_placed_order_rate;
    private int average_days_to_repeat_placed_order;
    private double repeat_paid_order_rate;
    private int average_days_to_repeat_paid_order;
    private double repeat_confirmed_order_rate;
    private int average_days_to_repeat_confirmed_order;

    private List<ModelData> models;
}
