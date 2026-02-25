#!/usr/bin/env bash

run_trino_sql() {
  podman exec -it trino trino --execute "$1" | sed 's/"//g; /^USE/d'
}

run_tpcds() {
  sf="$1"
  query="$2"
  run_trino_sql "$(printf "use tpcds.sf%s; %s" "$sf" "$query")"
}

sf=1

#run_tpcds "$sf" "select 'item', count(*) from item;" | tail
#run_tpcds "$sf" "select 'date_dim', count(*) from date_dim;" | tail

#run_tpcds "$sf" "select i_current_price,count(*),i_current_price IS NULL from item group by i_current_price order by i_current_price;" | tail

dump_column() {
  sf="$1"
  table="$2"
  column="$3"
  printf "dumping %s.%s, sf%s\n" "$table" "$column" "$sf"
  run_tpcds "$sf" "select $column from $table where $column IS NOT NULL" | zstd -15 > "sf$sf/$table.$column.zstd"
}

dump_column "$sf" item i_item_sk
dump_column "$sf" item i_rec_start_date
dump_column "$sf" item i_rec_end_date

dump_column "$sf" item i_current_price
dump_column "$sf" item i_wholesale_cost
dump_column "$sf" item i_brand_id
dump_column "$sf" item i_class_id
dump_column "$sf" item i_category_id
dump_column "$sf" item i_manufact_id
dump_column "$sf" item i_manager_id


dump_column "$sf" store_sales ss_sold_time_sk
dump_column "$sf" store_sales ss_item_sk
dump_column "$sf" store_sales ss_customer_sk
dump_column "$sf" store_sales ss_cdemo_sk
dump_column "$sf" store_sales ss_hdemo_sk
dump_column "$sf" store_sales ss_addr_sk
dump_column "$sf" store_sales ss_store_sk
dump_column "$sf" store_sales ss_promo_sk
dump_column "$sf" store_sales ss_ticket_number
dump_column "$sf" store_sales ss_quantity
dump_column "$sf" store_sales ss_wholesale_cost
dump_column "$sf" store_sales ss_list_price
dump_column "$sf" store_sales ss_sales_price
dump_column "$sf" store_sales ss_ext_discount_amt
dump_column "$sf" store_sales ss_ext_sales_price
dump_column "$sf" store_sales ss_ext_wholesale_cost
dump_column "$sf" store_sales ss_ext_list_price
dump_column "$sf" store_sales ss_ext_tax
dump_column "$sf" store_sales ss_coupon_amt
dump_column "$sf" store_sales ss_net_paid
dump_column "$sf" store_sales ss_net_paid_inc_tax
dump_column "$sf" store_sales ss_net_profit


