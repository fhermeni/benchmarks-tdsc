#!/usr/bin/Rscript
require(Hmisc);

pdf('filter-actions.pdf', height=3, width=5)
# Trim off excess margin space (bottom, left, top, right)
par(mar=c(3.1, 3.9, 0.2, 0.5),ps=14,mgp=c(1.7,0.6,0))

relocs <- read.table("data/filter-relocs.data",sep='\t',header=T)
run <- read.table("data/filter-starts.data",sep='\t',header=T)
migr <- read.table("data/filter-migs.data",sep='\t',header=T)

rebuildRelocs <- read.table("data/filter-relocs.rebuild.data", sep='\t', header=T)
rebuildRun <- read.table("data/filter-starts.rebuild.data", sep='\t', header=T)
rebuildMigr <- read.table("data/filter-migs.rebuild.data", sep='\t', header=T)

sizesRepair <- relocs[,1];
actionsRepair <- relocs + run + migr;

sizesRebuild <- rebuildRelocs[,1];
actionsRebuild <- rebuildRelocs + rebuildRun + rebuildMigr;

plot(1,type="n",axes=F,xlab="",ylab="",xlim=c(15,30),ylim=c(0,1600))

pchs <- c(4,3)
colors = gray(c(0.2,0.6));
lwds <- c(5,5)

print(actionsRebuild[3]);
print(actionsRepair[,3]);
lines(sizesRebuild, actionsRebuild[2], lwd=lwds[1], type="o", pch=pchs[1], col=colors[1])
lines(t(sizesRepair), actionsRepair[,3], lwd=lwds[1], type="o", pch=pchs[1], col=colors[1])
lines(t(sizesRepair), actionsRepair[,2], lwd=lwds[2], type="o", pch=pchs[2], col=colors[2])
lines(sizesRebuild, actionsRebuild[3], lwd=lwds[3], type="o", pch=pchs[3], col=colors[3])

axis(1,seq(15,30,by=5))
axis(2,seq(0,1600,by=200),las=1)

title(xlab="Virtual machines (per 1,000)",ylab="Actions\n")

legend("topleft",c("NR-filter","LI-filter"),col=colors,lwd=lwds,bty="n",pch=pchs)
dev.off()