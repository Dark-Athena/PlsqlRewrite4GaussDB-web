create or replace package body test_array_type_ele_pck is
procedure p is
i number:=1;
i_tyt tyt:=tyt();
l_var number;
begin
i_tyt.extend;
i_tyt(1).a1:=1;
i_tyt(1).a8:=1;
i_tyt(1).a9:=1;
i_tyt(1).a10:=1;
for rec in (select a from t where t.c=i_tyt(i).a1) loop
insert into t select a from t where t.c=i_tyt(i).a2;
update t set a=i_tyt(i).a3 where a=i_tyt(i).a4;
delete t where a=i_tyt(i).a5;
select count(1) into l_var from t where t.c=i_tyt(i).a6;
select i_tyt(i).a7 into l_var from t where t.c=i_tyt(i).a8 and i_tyt(i).a9=i_tyt(i).a10;
select i_tyt(i).a11 into l_var;
insert into t(a) values (i_tyt(i).a12);
select 1 into i_tyt(i).a13;
i_tyt(i).a14:=1;
end loop;
end;
end;
/

declare
 type tyt_test2 is table of ty_test2 index by varchar2;
 l_tyt_test2 tyt_test2;
 l_cnt number;
 begin
     l_tyt_test2('cc').begindate:=null;
     l_tyt_test2('cc').enddate:=null;
     select count(case when l_tyt_test2('cc').begindate is null and l_tyt_test2('cc').enddate is null then 1 end) 
          into l_cnt
          from t_test2 where id='cc' and rownum<=100000;
 end;
 /