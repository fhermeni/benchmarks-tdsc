package planStats;

use strict;
use Exporter;
use vars qw($VERSION @ISA @EXPORT @EXPORT_OK %EXPORT_TAGS);

$VERSION = 1.00;
@ISA = qw(Exporter);
@EXPORT = qw(%nodes %backtracks %costs %durations %ids %nbActions %applications %generations parseFiles averages);

our %nodes;
our %backtracks;
our %costs;
our %durations;
our %ids;

our %nbActions;
our %nbMigrations;
our %nbRelocations;
our %nbStarts;
our %applications;
our %generations;


sub parseFiles {
    my (@files) = @_;
#Parse each file
for (my $i = 0; $i < scalar(@files); $i++) {
    my $k = $files[$i];
    open(FP,"$k/results.data") or die("Unable to read '$k/results.data'\n");
    while(my $line = <FP>) {
	my ($id,$gen,$cost,$n,$b,$ms,$nbAction,$nbMigs,$nbInst,$nbStart,$apply) = split(' ', $line);
	push @{$costs{$k}{$id}}, $cost;
	push @{$nodes{$k}{$id}}, $n;
	push @{$backtracks{$k}{$id}}, $b;
	push @{$durations{$k}{$id}}, $ms;

	$generations{$k}{$id} = $gen;
	$applications{$k}{$id} = $apply;
	$nbActions{$k}{$id} = $nbAction;
	$nbMigrations{$k}{$id} = $nbMigs;
	$nbRelocations{$k}{$id} = $nbInst;
	$nbStarts{$k}{$id} = $nbStart;
	
	$ids{$id} = 1;
    }
    close(FP);
}
}

sub averages {
    my ($k) = @_;
    my %stats;
    $stats{nbSols} = 0;
    $stats{solvDuration} = 0;
    $stats{lastCost} = 0;
    $stats{firstCost} = 0;
    $stats{lastTime} = 0;
    $stats{firstTime} = 0;
    $stats{nbActions} = 0;
    $stats{nbMigrations} = 0;
    $stats{nbRelocations} = 0;
    $stats{nbStarts} = 0;

    $stats{apply} = 0;

    my $sumSuccess = 0;
    my $sumNodes = 0;
    my $sumApply = 0;
    my $sumActions = 0;
    my $sumMigs = 0;
    my $sumStarts = 0;
    my $sumRelocations = 0;
    my $sumLastCost = 0;
    my $sumFirstCost = 0;
    my $sumFirstNodes = 0;
    my $sumFirstFails = 0;
    my $sumLastTime = 0;
    my $sumFirstTime = 0;
    my $sumLastNodes = 0;
    my $sumLastFails = 0;
    my $nbSuccess = 0;
    my $sumNbSols = 0;
    my $sumSolvDuration = 0;
    my $sumGen = 0;
    my $nb = 0;#scalar(keys(%{$nbActions{$k}}));
    foreach my $id (keys(%{$nbActions{$k}})) {
	if (defined($nbActions{$k}{$id}) && ($nbActions{$k}{$id} ne "-")) {
	    $sumSuccess++;
	    $sumActions += $nbActions{$k}{$id};
	    $sumMigs += $nbMigrations{$k}{$id};
	    $sumStarts += $nbStarts{$k}{$id};
	    $sumRelocations += $nbRelocations{$k}{$id};
	    $sumApply += $applications{$k}{$id};
	    $sumSolvDuration += $durations{$k}{$id}[-1];
	    if (defined($generations{$k}{$id})) {
		$sumSolvDuration += $generations{$k}{$id};
		$sumGen += $generations{$k}{$id};
	    }
	    $sumFirstCost += $costs{$k}{$id}[0];
	    $sumLastCost += $costs{$k}{$id}[-1];
	    $sumFirstTime += $durations{$k}{$id}[0];
	    $sumLastTime += $durations{$k}{$id}[-1];
	    $sumFirstNodes += $nodes{$k}{$id}[0];
	    $sumLastNodes += $nodes{$k}{$id}[-1];
	    $sumFirstFails += $backtracks{$k}{$id}[0];
	    $sumLastFails += $backtracks{$k}{$id}[-1];
	    $sumNbSols += scalar(@{$costs{$k}{$id}});
	} else {
#	    print STDERR "undefined value for $k id=$id\n";
	}$nb++;
    }
#    print STDERR "for $k, nb = $sumSuccess\n";
    $stats{success} = $sumSuccess > 0 ? 100 * $sumSuccess / $nb : 0;
    $stats{nbActions} = $sumSuccess > 0 ? sprintf("%.1f", $sumActions / $sumSuccess) : "-";
    $stats{nbMigrations} = $sumSuccess > 0 ? sprintf("%.1f", $sumMigs / $sumSuccess) : "-";
    $stats{nbStarts} = $sumSuccess > 0 ? sprintf("%.1f", $sumStarts / $sumSuccess)  : "-";
    $stats{nbRelocations} = $sumSuccess > 0 ? sprintf("%.1f", $sumRelocations / $sumSuccess) : "-";
    $stats{apply} = $sumSuccess > 0 ? sprintf("%.1f", $sumApply / $sumSuccess) : "-";
    $stats{lastCost} = $sumSuccess > 0 ? int(($sumLastCost / $sumSuccess)) : "-";
    $stats{firstCost} = $sumSuccess > 0 ? int(($sumFirstCost / $sumSuccess))  : "-";
    $stats{lastTime} = $sumSuccess > 0 ? sprintf("%.1f", $sumLastTime / $sumSuccess / 1000) : "-";
    $stats{firstTime} = $sumSuccess > 0 ? sprintf("%.1f", $sumFirstTime / $sumSuccess / 1000) : "-";
    $stats{generation} = $sumSuccess > 0 ? sprintf("%.1f", $sumGen / $sumSuccess / 1000) : "-";
    $stats{lastNodes} = $sumSuccess > 0 ? int($sumLastNodes / $sumSuccess) : "-";
    $stats{firstNodes} = $sumSuccess > 0 ? int($sumFirstNodes / $sumSuccess) : "-";
    $stats{lastFails} = $sumSuccess > 0 ? int($sumLastFails / $sumSuccess) : "-";
    $stats{firstFails} = $sumSuccess > 0 ? int($sumFirstFails / $sumSuccess) : "-";
    $stats{nbSols} = $sumSuccess > 0 ? sprintf("%.1f", $sumNbSols / $sumSuccess) : 0;
    $stats{solvDuration} = $sumSuccess > 0 ? sprintf("%.1f", $sumSolvDuration / $sumSuccess / 1000) : "-";
    $stats{generation} = $sumSuccess > 0 ? sprintf("%.1f", $sumGen / $sumSuccess / 1000) : "-";
    return %stats;
}

1;
