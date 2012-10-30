#!/usr/bin/Rscript
require(Hmisc);

args <- commandArgs(TRUE);
input <- args[1];
output <- args[2];

pdf(paste(output,'/filter-duration.pdf',sep=""), height=3, width=5)
# Trim off excess margin space (bottom, left, top, right)
par(mar=c(2.8, 4.2, 0.2, 0.2),ps=14,mgp=c(1.6,0.6,0))

durRepair <- read.table(paste(input,"/filter-duration.data",sep=""),sep='\t',header=T)
durRebuild <- read.table(paste(input,"/wofilter-duration.data", sep=""), sep='\t', header=T)

applyRepair <- rfead.table(paste(input,"/filter-apply.data",sep=""), sep='\t',header=T)
applyRebuild <- read.table(paste(input,"/wofilter-apply.data",sep=""), sep='\t', header=T)

sizes <- durRepair[,1];
sizesRebuild <- durRebuild[,1];
plot(1,type="n",axes=F,xlab="",ylab="",xlim=c(15,30),ylim=c(0,310))

pchs <- c(1,2,3,4)

colors = gray(c(0.6,0.2,0.6,0.2));
lwds <- c(3,3,5,5);
print(durRebuild[,2]);
print(applyRebuild[,3]);
lines(sizesRebuild, t(durRebuild[,2] + applyRebuild[,2]), lwd=lwds[1], type="o", pch=pchs[1], col=colors[1])
lines(sizesRebuild, t(durRebuild[,3] + applyRebuild[,3]), lwd=lwds[2], type="o", pch=pchs[2], col=colors[2])

lines(sizes, t(durRepair[,2] + applyRepair[,2]), lwd=lwds[3], type="o", pch=pchs[3], col=colors[3])
lines(sizes, t(durRepair[,3] + applyRepair[,3]), lwd=lwds[4], type="o", pch=pchs[4], col=colors[4])


axis(1,seq(15,30,by=5))
axis(2,seq(0,310,by=60),las=1)
#minor.tick(nx=1, ny=2, tick.ratio=0.5)

title(xlab="Virtual machines (per 1,000)",ylab="Time (sec)\n")

legend("topright",c("LI","NR","LI-filter","NR-filter"),col=colors,lwd=lwds,bty="n",pch=pchs)
dev.off()