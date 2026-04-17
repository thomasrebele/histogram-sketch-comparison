#!/usr/bin/env bash

run_trino_sql() {
  podman exec -it trino trino --execute "$1" | sed 's/"//g; /^USE/d'; echo "#done"
}

run_tpcds() {
  dbname="$1"
  query="$2"
  run_trino_sql "$(printf "use tpcds.%s; %s" "$dbname" "$query")"
}


#run_tpcds "$sf" "select 'item', count(*) from item;" | tail
#run_tpcds "$sf" "select 'date_dim', count(*) from date_dim;" | tail

#run_tpcds "$sf" "select i_current_price,count(*),i_current_price IS NULL from item group by i_current_price order by i_current_price;" | tail

dump_column() {
  sf="$1"
  table="$2"
  column="$3"
  dbname="$sf"
  if [[ "$sf" == [0-9]* ]]; then
    dbname="sf$sf"
  fi
  outdir="$dbname"
  outfile="$outdir/$table.$column.zstd"

  if [ -f "$outfile" ]; then
    if [[ "$(zstdcat "$outfile" | tail -n 1)" == "#done" ]]; then
      printf "already done %s.%s, %s\n" "$table" "$column" "$dbname"
      return
    fi
  fi
  printf "dumping %s.%s, %s\n" "$table" "$column" "$dbname"
  mkdir -p "$outdir"
  run_tpcds "$dbname" "select $column from $table where $column IS NOT NULL" | zstd -15 > "$outfile"
}

sf=tiny
# size indepedent
dump_column "$sf" date_dim d_date
dump_column "$sf" date_dim d_month_seq
dump_column "$sf" date_dim d_year
dump_column "$sf" date_dim d_qoy
dump_column "$sf" date_dim d_dom
dump_column "$sf" date_dim d_moy
dump_column "$sf" household_demographics hd_vehicle_count
dump_column "$sf" income_band ib_upper_bound
dump_column "$sf" income_band ib_lower_bound
dump_column "$sf" time_dim t_time
dump_column "$sf" time_dim t_minute
dump_column "$sf" time_dim t_hour

sf=1
dump_column "$sf" inventory inv_quantity_on_hand
dump_column "$sf" store_sales ss_ext_list_price
dump_column "$sf" store_sales ss_ext_wholesale_cost
dump_column "$sf" store_sales ss_net_profit
dump_column "$sf" store_sales ss_net_paid_inc_tax
dump_column "$sf" store_sales ss_ext_sales_price
dump_column "$sf" store_sales ss_net_paid
dump_column "$sf" store_sales ss_list_price
dump_column "$sf" store_sales ss_ext_tax
dump_column "$sf" store_sales ss_wholesale_cost
dump_column "$sf" store_sales ss_sales_price
dump_column "$sf" catalog_sales cs_net_profit
dump_column "$sf" catalog_sales cs_net_paid
dump_column "$sf" store_sales ss_promo_sk
dump_column "$sf" store_sales ss_quantity
dump_column "$sf" web_sales ws_net_profit
dump_column "$sf" web_sales ws_net_paid
dump_column "$sf" store_sales ss_coupon_amt
dump_column "$sf" store_sales ss_ext_discount_amt
dump_column "$sf" catalog_sales cs_quantity
dump_column "$sf" store_sales ss_cdemo_sk
dump_column "$sf" store_sales ss_customer_sk
dump_column "$sf" store_sales ss_item_sk
dump_column "$sf" store_sales ss_addr_sk
dump_column "$sf" store_sales ss_sold_time_sk
dump_column "$sf" store_sales ss_hdemo_sk

sf=10
dump_column "$sf" store_returns sr_return_amt
dump_column "$sf" web_sales ws_quantity
dump_column "$sf" catalog_returns cr_return_amount
dump_column "$sf" store_sales ss_ticket_number
dump_column "$sf" store_sales ss_store_sk
dump_column "$sf" web_returns wr_return_amt

sf=100
dump_column "$sf" customer_address ca_zip

sf=100000
dump_column "$sf" item i_current_price
dump_column "$sf" item i_wholesale_cost
dump_column "$sf" item i_brand_id
dump_column "$sf" item i_manufact_id
dump_column "$sf" item i_manager_id
dump_column "$sf" item i_item_sk
dump_column "$sf" item i_category_id
dump_column "$sf" item i_class_id
dump_column "$sf" item i_rec_start_date
dump_column "$sf" web_page wp_char_count
dump_column "$sf" store s_number_employees
dump_column "$sf" item i_rec_end_date
