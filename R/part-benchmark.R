#!/usr/bin/Rscript
require(Hmisc);
pdf('nbParts.pdf', height=3, width=5)
# Trim off excess margin space (bottom, left, top, right)
par(mar=c(3.1, 3.9, 0.2, 0.5),ps=16,mgp=c(2.2,0.6,0))

apply <- read.table("data/nbParts.data",header=F)

sizes <- apply[,1];
dur <- apply[,2] / 1000;

plot(sizes,dur, axes=F,xlab="",ylab="Time (sec)",xlim=c(0,28),ylim=c(0,50),type="o",lwd=5)

axis(1,seq(0,28,by=4))
axis(2,seq(0,50,by=10),las=1)
minor.tick(nx=1, ny=2, tick.ratio=0.5)

#box()
mtext("Partitions",line=-14.5);
legend("topleft","Partitioning duration",col="black",lwd=3,bty="n")
dev.off()