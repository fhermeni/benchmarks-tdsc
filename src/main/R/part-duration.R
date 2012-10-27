#!/usr/bin/Rscript

pdf('part-duration.pdf', height=3, width=5)
# Trim off excess margin space (bottom, left, top, right)
par(mar=c(3.1, 3.9, 0.2, 0.5),ps=16,mgp=c(2.3,0.6,0))

apply <- read.table("data/parts-apply.data",sep='\t',header=T)
dur <- read.table("data/parts-duration.data",sep='\t',header=T)

sizes <- apply[,1];
plot(1,type="n",axes=F,xlab="",ylab="",xlim=c(0,5000),ylim=c(0,150))

pchs <- c(4,3);

colors = gray(c(0.2,0.6));

    lines(sizes, dur[,2], lwd=5, type="o", pch=pchs[2], col=colors[2])
    lines(sizes, dur[,3], lwd=5, type="o", pch=pchs[1], col=colors[1])

axis(1,seq(0,5000,by=1000))
axis(2,seq(0,150,by=30),las=1)

#box()

title(ylab="Time (sec)")
mtext("Partition size (servers)",line=-14.6);

legend("topleft",c("NR + filter", "LI + filter"),col=colors,lwd=5,bty="n",pch=pchs)
dev.off()