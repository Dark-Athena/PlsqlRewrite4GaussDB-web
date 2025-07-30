select 1 from t group by 'constant string';
select 1 from t group by 'constant string' having count(*) > 0;
select 1 from t group by 'constant string' order by 1;
select 1 from t group by 'constant string', 'another constant string';
select 1 from t group by 'constant string', col1;
select 1 from t group by 'constant string', col1, col2;
select 1 from t group by 'constant string', col1, 'another constant string';
select 1 from t group by  col1, 'constant string', col2;
select 1 from t group by  col1,  col2, 'constant string';
select 1 from t group by  col1,  col2, 'constant string','another constant string';
select 1 from t group by  col1,  col2, 1,'constant string','another constant string';
select 1 from t having count(*) > 0 group by 'constant string';
select 1 from t group by  col1,  col2;

