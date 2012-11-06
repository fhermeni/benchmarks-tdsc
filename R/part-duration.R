#!/usr/bin/Rscript

args <- commandArgs(TRUE);
input <- args[1];
output <- args[2];

pdf(paste(output,'/partitions-duration.pdf',sep=""), height=3, width=5);


# Trim off excess margin space (bottom, left, top, right)
par(mar=c(3.1, 3.9, 0.2, 0.5),ps=16,mgp=c(2.3,0.6,0))

dur <- read.table(paste(input,"/partitions-duration.data",sep=""),sep='\t',header=T)

sizes <- dur[,1];
plot(1,type="n",axes=F,xlab="",ylab="",xlim=c(0,5000),ylim=c(0,150))

pchs <- c(4,3);

colors = gray(c(0.2,0.6));

    lines(sizes, dur[,9], lwd=5, type="o", pch=pchs[1], col=colors[1])
    lines(sizes, dur[,8], lwd=5, type="o", pch=pchs[2], col=colors[2])

axis(1,seq(0,5000,by=1000))
axis(2,seq(0,150,by=30),las=1)

title(ylab="Time (sec)")
mtext("Partition size (servers)",line=-14.6);

legend("topleft",c("LI + filter","NR + filter"),col=gray(c(0.6,0.2)),lwd=5,bty="n",pch=c(3,4))
dev.off()