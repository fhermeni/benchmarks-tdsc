#!/usr/bin/Rscript
require(Hmisc);

args <- commandArgs(TRUE);
input <- args[1];
output <- args[2];

pdf(paste(output,'/filter-apply.pdf',sep=""), height=3, width=5)
# Trim off excess margin space (bottom, left, top, right)
par(mar=c(2.8, 4.2, 0.2, 0.2),ps=17,mgp=c(2.3,0.6,0))

applyRepair <- read.table(paste(input,"/filter-apply.data",sep=""), sep='\t',header=T)
applyRebuild <- read.table(paste(input,"/wofilter-apply.data",sep=""), sep='\t', header=T)

sizes <- applyRepair[,1];
sizesRebuild <- applyRebuild[,1];
plot(1,type="n",axes=F,xlab="",ylab="",xlim=c(15,30),ylim=c(5,25))

pchs <- c(1,2,3,4)

colors = gray(c(0.6,0.2,0.6,0.2));
lwds <- c(3,3,5,5);
print(applyRebuild);
print(applyRepair);

lines(sizesRebuild, t(applyRebuild[,2] - 1), lwd=lwds[1], type="o", pch=pchs[1], col=colors[1])
lines(sizesRebuild, t(applyRebuild[,3] - 1), lwd=lwds[2], type="o", pch=pchs[2], col=colors[2])

lines(sizes, t(applyRepair[,2] - 1), lwd=lwds[3], type="o", pch=pchs[3], col=colors[3])
lines(sizes, t(applyRepair[,3] - 1), lwd=lwds[4], type="o", pch=pchs[4], col=colors[4])


axis(1,seq(15,30,by=5))
axis(2,seq(5,25,by=5),las=1)

title(ylab="Time (sec)");
mtext("Virtual machines (x 1,000)",line=-14.7);

legend("topleft",c("LI","NR","LI-filter","NR-filter"),col=colors,lwd=lwds,bty="n",pch=pchs)
dev.off()