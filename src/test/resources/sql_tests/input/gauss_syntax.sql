--本文件用于测试解析GaussDB的sql语法和gsql的元命令
--解析pg元命令
\i test.sql;
\ir test.sql
\q

--解析pg set命令
set search_path = 'abc';
set enable_nestloop to on;

--解析pg typecast
select '1'::text from t;
select '1'::int from t;
select 1 from t where a::text='1';
select substr(a,1,2)::int from t;
select (a)::int from t;

--解析limit offset fetch
select * from dual order by 1 limit 1;
select * from dual order by 1 limit 1,1;
select * from dual order by 1 limit 1,all;
select * from dual order by 1 limit all;
select * from dual order by 1 limit 1 offset 1 row;
select * from dual order by 1 limit 1 offset 1 rows;
select * from dual order by 1 offset 1 row limit 1 ;
select * from dual order by 1 offset 1 row fetch first 1 row only ;
select * from dual order by 1 fetch first 1 rows only;
select * from dual order by 1 fetch first 1 row only offset 1 rows ;
select * from dual order by 1 fetch first 1 rows only offset 1 row;
select * from dual order by 1 fetch next 1 row only offset 1 rows ;

--解析on duplicate key update(暂不支持解析)
/*
create table test_Replace(a int primary key,b int,c int);
insert into test_Replace values (1,1,1);
insert into test_Replace values (1,2,3) on duplicate key update nothing; --update nothing 
insert into test_Replace values (1,2,3) on duplicate key update b=2;  -- set_clause_list single_set_clause  set_target '=' ctext_expr
insert into test_Replace values (1,2,3) on duplicate key update b=2,c=2; --set_clause_list single_set_clause   set_target '=' ctext_expr
insert into test_Replace values (1,2,3) on duplicate key update b=values(b); --set_clause_list single_set_clause set_target '=' VALUES '(' columnref ')'
insert into test_Replace values (1,2,3) on duplicate key update (b)=(values(b)); --multiple_set_clause set_target '=' VALUES '(' columnref ')'  实际上是single_set_clause，未指定函数或者类型宣告的括号内如果只有一个表达式，则括号自动去掉
insert into test_Replace values (1,2,3) on duplicate key update (b,c)=(2,3); --multiple_set_clause '(' set_target_list ')' '=' ctext_row 
insert into test_Replace values (1,2,3) on duplicate key update (b,c)=(select 1,2 from dual); --multiple_set_clause '(' set_target_list ')' '=' '(' SELECT hint_string opt_distinct target_list from_clause where_clause group_clause having_clause ')'
*/