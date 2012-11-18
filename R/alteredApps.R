#!/usr/bin/Rscript
require(Hmisc);
args <- commandArgs(TRUE);
input <- args[1];
output <- args[2];
pdf(paste(output,"/alteredApps.pdf",sep=""), height=3, width=5)
# Trim off excess margin space (bottom, left, top, right)
par(mar=c(3.1, 3.9, 0.2, 0.5),ps=17,mgp=c(2.7,0.6,0))

pct <- read.table(paste(input,"/impacted.data",sep=""),sep=' ',header=F);
plot(1,type="n",axes=F,xlab="",ylab="",xlim=c(15,30),ylim=c(0,100))

lines(pct[,1],pct[,2], lwd=4, type="o", pch=1, col="black");

axis(1,seq(15,30,by=5))
axis(2,seq(0,100,by=20),las=1)
minor.tick(nx=1, ny=2, tick.ratio=0.5)

title(,ylab="% affected applications")
mtext("Virtual machines (x 1,000)",line=-14.7);

dev.off()