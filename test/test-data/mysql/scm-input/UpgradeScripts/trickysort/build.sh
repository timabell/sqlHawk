rm *.sql
for x in a b c 1 2 3 8 9 01 02 03 07 08 09 a_foo 2_foo 2_xy 98 99 100 101; do echo $x; echo "insert into testlog (name) values ('$x');" > $x.sql; done

