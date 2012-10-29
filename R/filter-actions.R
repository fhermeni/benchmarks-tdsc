#!/usr/bin/Rscript
require(Hmisc);

args <- commandArgs(TRUE);
input <- args[1];
output <- args[2];

pdf(paste(output,'/filter-actions.pdf',sep=""), height=3, width=5)
# Trim off excess margin space (bottom, left, top, right)
par(mar=c(2.8, 4.2, 0.2, 0.2),ps=14,mgp=c(2.6,0.6,0))

aRepair <- read.table(paste(input,"/filter-actions.data",sep=""),sep='\t',header=T)
aRebuild <- read.table(paste(input,"/wofilter-actions.data", sep=""), sep='\t', header=T)

sizes <- aRepair[,1];
sizesRebuild <- aRebuild[,1];
plot(1,type="n",axes=F,xlab="",ylab="",xlim=c(15,30),ylim=c(0,1500))

pchs <- c(1,2,3,4)

colors = gray(c(0.6,0.2,0.6,0.2));
lwds <- c(3,3,5,5);
lines(sizesRebuild, t(aRebuild[,2]), lwd=lwds[1], type="o", pch=pchs[1], col=colors[1])
lines(sizesRebuild, t(aRebuild[,3]), lwd=lwds[2], type="o", pch=pchs[2], col=colors[2])
lines(sizes, t(aRepair[,2]), lwd=lwds[3], type="o", pch=pchs[3], col=colors[3])
lines(sizes, t(aRepair[,3]), lwd=lwds[4], type="o", pch=pchs[4], col=colors[4])


axis(1,seq(15,30,by=5))
axis(2,seq(0,1500,by=250),las=1)

title(ylab="Actions")
mtext("Virtual machines (per 1,000)",line=-14.5);

legend("right",c("LI","NR","LI-filter","NR-filter"),col=colors,lwd=lwds,bty="n",pch=pchs)
dev.off()