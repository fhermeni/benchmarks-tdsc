#!/usr/bin/perl -w
use planStats;
use strict;
use File::Basename;
use List::Util qw(sum);
use POSIX qw/strftime/;


#Usage: report.pl datafile1 datafile2 ... datafile3
if (scalar(@ARGV) == 0) {
    print "Usage: report.pl file1 file2 ... fileN\n";
    print "Summarize all the given files in an HTML report printed on the standard output\n";
    exit 1;
}

parseFiles(@ARGV);


print "<?xml version=\"1.0\" encoding=\"iso-8859-15\"?>";

print "<html>";
print "<head>
<script type=\"text/javascript\">

function toggleView() {
  var text = document.getElementById('viewToggle');
  if (text.innerHTML == \"Switch to the solving process view\") {
     text.innerHTML = \"Switch to the result view\";
     var elems = document.getElementsByClassName('st');
     for (var i = 0; i < elems.length; i++) {
        elems[i].style.display = \"block\";
     }
     elems = document.getElementsByClassName('sol');
     for (var i = 0; i < elems.length; i++) {
        elems[i].style.display = \"none\";
     }
  } else {
     text.innerHTML = \"Switch to the solving process view\";
     var elems = document.getElementsByClassName('st');
     for (var i = 0; i < elems.length; i++) {
        elems[i].style.display = \"none\";
     }
     elems = document.getElementsByClassName('sol');
     for (var i = 0; i < elems.length; i++) {
        elems[i].style.display = \"block\";
     }
  }
}

function toggleResult(id) {
	btn   = document.getElementById('chk-' + id);
        mode = btn.checked ? 'table-cell' : 'none';
        elems = document.getElementsByClassName('col' + id);
        for (var i = 0; i < elems.length; i++) {
          elems[i].style.display = mode;
        }
        mode = btn.checked ? '' : 'none';
        elems = document.getElementsByClassName('row' + id);
        for (var i = 0; i < elems.length; i++) {
          elems[i].style.display = mode;
        }

}
</script>
<style type=\"text/css\">
body {
  font-size: 12pt;
  text-align: center;
}

.st {
 display: none;
}

table {
    border-collapse: collapse;
}

td {
    text-align: center;
    vertical-align: top;
    padding-left: 4px;
    padding-right: 4px;
}

.light {
  color: #AAA;
}
th {
  background: #CCC;
    padding-left: 5px;
    padding-right: 5px;
}

.k {
 text-align: left;
 vertical-align: middle;
 padding-right: 10px;
 font-weight: bold;
}

tr:nth-child(odd) {
background: #FFF;
}

tr:nth-child(even) {
background: #EEE;
}

.legend {
    font-size: 10pt;
}

.banner {
  width: 800px;
    text-align: left;
}

caption {
    text-align: left;
    font-size: 10pt;
}

li{
list-style-type: none;
}

li:before {
   content: \"-\";
}
</style>
</head>";


print "<body><div align=\"center\">";
print "<h1>Plan bench results on ";
print strftime('%d %b %Y, %H:%M',localtime);
print "</h1>\n";

print "<div class=\"banner\">";
print "<a href=\"#\" id=\"viewToggle\" onclick=\"toggleView(); return false;\">Switch to the solving process view</a><br>";
print "<b>Show results for: </b><br/>";
my $x = 0;
foreach my $k (sort keys(%nodes)) {
print " <input  type=\"checkbox\" id=\"chk-$k\" onclick=\"toggleResult(\'$k\');\" checked=\"true\"> $k";
if ($x++  % 3 == 0) {
    print "<br/>";
}
}
print "</div>";

### RESULT OVERVIEW ###

print "<h2>Result overview</h2>";

print "<div class=\"sol\">";
print "<table class=\"summary\">";
print "<caption>".
    "<ul>".
    "<li>Solving duration: time to generate the problem + time to get the last solution (in seconds)</li>".
    "<li>Apply: estimated application time of the plan (in seconds)</li>".
    "</ul>".
    "</caption>";
print "<tr>";
print "<th/>".
    "<th>Solved</th>".
    "<th>Solving duration</th>".
    "<th>Nb. actions</th>".
    "<th>Apply duration</th>".
    "<th>Cost</th>";
print "</tr>";

my $i = 0;
for my $k (sort keys(%nodes)) {
    print "<tr  class=\"row$k\">";
    my %stats = averages($k);
    print "<td class=\"k\">$k</td>".
	  "<td>$stats{success}</td>".	  
	  "<td>$stats{solvDuration}</td>".
	  "<td>$stats{nbActions}</td>".
	  "<td>$stats{apply}</td>".
	  "<td>$stats{lastCost}</td>";
    print "</tr>";
    $i++;
}
print "</table>";
print "</div>";
print "<div class=\"st\">";
print "<table class=\"summary\">";
print "<caption>".
    "<ul>".
    "<li>Generation: time to generate the problem (in seconds)</li>".
    "<li>Apply: estimated application time of the plan (in seconds)</li>".
    "</ul>".
    "</caption>";
print "<tr>";
print "<th rowspan=\"2\" class=\"k\"/>".
    "<th rowspan=\"2\">Solved</th>".
    "<th rowspan=\"2\">Solutions</th>".
    "<th rowspan=\"2\">Generation</th>".
    "<th colspan=\"4\">First solution</th>".
    "<th colspan=\"4\">Last solution</th>";
print "</tr>";
print "<tr>";
print "<th style=\"padding-left: 10px;\">cost</th><th>nodes</th><th>fails</th><th style=\"padding-right: 30px;\">time</th><th>cost</th><th>nodes</th><th>fails</th><th>time</th>";
print "</tr>";

$i = 0;
for my $k (sort keys(%nodes)) {
print "<tr class=\"row$k\">";
    my %stats = averages($k);
    print "<td class=\"k\">$k</td>".
	  "<td>$stats{success}</td>".	  
	  "<td>$stats{nbSols}</td>".
	  "<td>$stats{generation}</td>".
	  "<td>$stats{firstCost}</td>".
	  "<td>$stats{firstNodes}</td>".
	  "<td>$stats{firstFails}</td>".
	  "<td>$stats{firstTime}</td>".
	  "<td>$stats{lastCost}</td>".
	  "<td>$stats{lastNodes}</td>".
	  "<td>$stats{lastFails}</td>".
	  "<td>$stats{lastTime}</td>";
    print "</tr>";
    $i++;
}
print "</table>";

print "</div>";
### RESULT DETAILS ###
print "<h2>Result details</h2>";


##Stats details
print "<div class=\"st\">";
print "<table class=\"detail\">";
print "<tr><th rowspan=\"2\" class=\"k\"/>";
foreach my $k (sort keys(%nodes)) {
    print "<th colspan=\"5\" class=\"col$k\">$k</th>";
}
print "</tr>";
print "<tr>";
foreach my $k (sort keys(%nodes)) {
    print "<th class=\"col$k\">gen (ms)</th><th  class=\"col$k\">obj</th><th  class=\"col$k\">nodes</th><th  class=\"col$k\">fails</th><th  class=\"col$k\" style=\"padding-right: 20px;\">time (ms)</th>";
}
print "</tr>";
$i = 0;
for my $id (sort(keys(%ids))) {
    print "<tr>";
    print "\t<td class=\"k\">$id</td>\n";
    foreach my $k (sort keys(%nodes)) {
	cellStatContent($id,$k);
    }
    $i++;
    print "</tr>";
}
print "</table>";
print "</div>";

##Results details
print "<div class=\"sol\">";
print "<table class=\"detail\">";
print "<tr><th rowspan=\"2\" class=\"k\"/>";
foreach my $k (sort keys(%nodes)) {
    print "<th colspan=\"3\" class=\"col$k\">$k</th>";
}
print "</tr>";
print "<tr>";
foreach my $k (sort keys(%nodes)) {
    print "<th class=\"col$k\">Actions</th><th  class=\"col$k\">Apply</th><th  class=\"col$k\" style=\"padding-right: 30px;\">Solving</th>";
}
print "</tr>";
$i = 0;
for my $id (sort(keys(%ids))) {
    print "<tr>";
    print "\t<td class=\"k\">$id</td>\n";
    foreach my $k (sort keys(%nodes)) {
	cellSolContent($id,$k);
    }
    $i++;
    print "</tr>";
}
print "</table>";
print "</div>";


print "</body></html>";

sub multilineCell {
    my (@content,$k) = @_;
    print "<div class=\"light\">";
	for (my $i = 0; $i < scalar(@content) - 1; $i++) {
	    print "$content[$i]<br/>";
	}
	    print "</div>";
	    print $content[-1];
}


sub cellSolContent {
    my ($id,$k) = @_;
    my $a = "-";
    my $t = "-";
    my $g = "-";
    my $d = "-";
    if (defined($nbActions{$k}{$id}) && ($nbActions{$k}{$id} ne "-")) {
	$a = $nbActions{$k}{$id};
	$t =  $applications{$k}{$id};
	$g = sprintf("%.1f",$generations{$k}{$id} / 1000);
	$d = sprintf("%.1f",$durations{$k}{$id}[-1] / 1000);
print "<td class=\"col$k\">$a</td><td  class=\"col$k\">$t</td><td  class=\"col$k\">".($g + $d)."</td>\n";
    } else {
	print "<td colspan=\"3\"  class=\"col$k\">-</td>";
    }
}
sub cellStatContent {
    my ($id,$k) = @_;
    if (defined($nbActions{$k}{$id}) && ($nbActions{$k}{$id} ne "-")) {
	print "<td class=\"col$k\">$generations{$k}{$id}</td>";
	print "<td class=\"col$k\">";
	  multilineCell(@{$costs{$k}{$id}});
	print "</td>";
	print "<td class=\"col$k\">";
	multilineCell(@{$nodes{$k}{$id}});
	print "</td>";

	print "<td class=\"col$k\">";
	multilineCell(@{$backtracks{$k}{$id}});
	print "</td>";

	print "<td class=\"col$k\">";
	multilineCell(@{$durations{$k}{$id}});
	print "</td>";

    } else {
	print "<td colspan=\"5\" class=\"col$k\">-</td>";
    }
}
