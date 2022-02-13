# flat tag
flat tag command flats tagged file like XML or HTML to flatten text file which treats by sed or awk easily.

## Command

### flattag

flattag converts tagged file to flat text file.
```
$ cat file1.xml
<table>
  <tr><td>1-1</td><td>1-2</td></tr>
  <tr><td>2-1</td><td>2-2</td></tr>
  <tr><td>3-1</td><td>3-2</td></tr>
</table>

$ flattag -d ';' file1.xml
table;   
table;tr;td;1-1
table;tr;td;1-2
table;tr;
table;   
table;tr;td;2-1
table;tr;td;2-2
table;tr;
table;   
table;tr;td;3-1
table;tr;td;3-2
table;tr;
table; 
```

## Examples

Remove all tags from HTML file.

```
flattag -n '\n' -t '\t' test1.html |
grep -v '\t\\n$' |
awk -F'\t' '{ print $NF }' |
perl -pe 's/\\n/\n/g' |
perl -pe 's/\\t/\t/g'
```

Result:
```


Document1
This is a document


```

where test1.html is shown as follows.
```
<!DOCTYPE html>
<html>
<body>
<div class="class1">
<h1>Document1</h1>
<p>This is a document</p>
</div>
</body>
</html>
```

Converts HTML like table to CSV file.
```
flattag idols.xml |
grep 't[dh]' |
awk -F'\t' '{ print $NF }' |
paste -d ',' - - - - -
```

Result:
```
id,name,age,height,place
1,KUDO Shinobu,16,154,Aomori
2,MOMOI Azuki,15,145,Nagano
3,AYASE Honoka,17,161,Miyagi
4,KITAMI Yuzu,15,156,Saitama
```

where idols.xml is shown as follows.
```
<table>
  <tr>
    <th>id</th>
    <th>name</th>
    <th>age</th>
    <th>height</th>
    <th>place</th>
  </tr>
  <tr>
    <td>1</td>
    <td>KUDO Shinobu</td>
    <td>16</td>
    <td>154</td>
    <td>Aomori</td>
  </tr>
  <tr>
    <td>2</td>
    <td>MOMOI Azuki</td>
    <td>15</td>
    <td>145</td>
    <td>Nagano</td>
  </td>
  <tr>
    <td>3</td>
    <td>AYASE Honoka</td>
    <td>17</td>
    <td>161</td>
    <td>Miyagi</td>
  <tr>
  </tr>
    <td>4</td>
    <td>KITAMI Yuzu</td>
    <td>15</td>
    <td>156</td>
    <td>Saitama</td>
  </tr>
</table>
```

