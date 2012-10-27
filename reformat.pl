#!/usr/bin/perl -w
use planStats;
use strict;

#A tool to extract statistics from a serie of directories containing
#result.data files
#Output is a tab delimited datafile that can be parsed by R.
#Addition of statistics are supported

if (scalar(@ARGV) == 0) {
    print "Usage: reformat.pl stat -xtics ticLabel t1,t2,t3 -d label1 file1 ... file3 -d label2 file4 ... file7 ...\n";
    exit(1);
}

my $DELIM = "\t";
my @lines;
my @stats = split(/\+/,$ARGV[0]);
my $stat = $ARGV[0];
my @tics;
my $ticsLabel;
my @data;
my @labels;
my $nb = 0;
for (my $i = 1; $i < scalar(@ARGV); $i++) {
    my $a = $ARGV[$i];
    if ($a eq "-xtics") {
	$ticsLabel = $ARGV[++$i];
	@tics = split(/,/,$ARGV[++$i]);
    } elsif ($a eq "-d") {	
	push @labels,$ARGV[++$i];
	my $x = 0;
	my $j = $i + 1;
	for (; $j < scalar(@ARGV); $j++) {
	    my $d = $ARGV[$j];
	    if ($d eq "-") {
		$data[$nb][$x] = $d;
	    } elsif ($d eq "-d") {
		last;
	    } else {
		parseFiles(($d));
		my $val = "";
		my %avgs = averages($d);
		foreach my $st (@stats) {
		    if (!defined($avgs{$st})) {
			print STDERR ("Unknown statistic: $stat\nAvailable: ".join(",",keys(%avgs))."\n");
			exit 1;
		    }
		   
		    if ($avgs{$st} ne "-") { #Missing datapoint
			$val = $val eq "" ? $avgs{$st} : ($val + $avgs{$st});
		    }
		}
		$data[$nb][$x] = $val;
	    }
	    $x++;
	}
	$nb++;
    }
}
if (length($ticsLabel) == 0) {
    print STDERR "No label for the datapoint(s)\n";
}

if (@labels == 0) {
    print STDERR "No label(s) given\n";
    exit 1;
}
print "$ticsLabel";
for (my $l = 0; $l < scalar(@labels); $l++) {
    print "$DELIM$labels[$l]";
}
print "\n";
for (my $i = 0; $i < scalar(@tics); $i++) {
    print $tics[$i];
    for (my $j = 0; $j < scalar(@labels); $j++) {
print "$DELIM";
	if (defined($data[$j][$i])) {
	    print $data[$j][$i];
	} else {
	    print "-";
	}
    }
    print "\n";
}
