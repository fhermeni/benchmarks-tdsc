#!/usr/bin/Rscript
args <- commandArgs(TRUE);
input <- args[1];
output <- args[2];

pdf(paste(output,'/solveImpact.pdf',sep=""), height=3, width=5);


# Trim off excess margin space (bottom, left, top, right)
par(mar=c(3, 3, 0.5, 0.1),ps=14,mgp=c(1.8,0.6,0))
dur <- read.table(paste(input,"/partitions-duration.data",sep=""),sep='\t',header=T)
apply <- read.table(paste(input,"/partitions-apply.data",sep=""),sep='\t',header=T)

inv<-c(5,4,3,2,1);
LIdur <- cbind(dur[,2],dur[,4],dur[,6],dur[,8]);
LIapply <- cbind(apply[,2],apply[,4],apply[,6],apply[,8]);

MTTR <- LIdur + LIapply - 1; #1 seconds due to the rename action


impact <- LIdur / MTTR * 100;


partSize <- c(250,500,1000,2500,5000);
consRatio <- c(15,20,25,30);

filled.contour(partSize,consRatio,impact[inv,],
      xlim=c(250,5000),ylim=c(15,30),zlim=c(0,100),
      xlab="",ylab="Virtual machines (x 1,000)",
      plot.axes = {axis(1,c(1000,2000,3000,4000,5000))
                   axis(2,consRatio,las=1)},
     col=gray(seq(1,0.3,length=10)),
     nlevels=10
);

mtext("Partition size (servers)                          ",line=-14);
mtext("% of the MTTR",side=4,line=-6);
dev.off()