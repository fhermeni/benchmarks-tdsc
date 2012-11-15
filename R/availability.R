#!/usr/bin/Rscript
args <- commandArgs(TRUE);
input <- args[1];
output <- args[2];

pdf(paste(output,'/availability.pdf',sep=""), height=3, width=5);


# Trim off excess margin space (bottom, left, top, right)
par(mar=c(3, 3, 0.5, 0.5),ps=13,mgp=c(1.5,0.6,0))
dur <- read.table(paste(input,"/partitions-duration.data",sep=""),sep='\t',header=T)
apply <- read.table(paste(input,"/partitions-apply.data",sep=""),sep='\t',header=T)
altered <- read.table(paste(input,"/impacted.data",sep=""),sep=' ',header=F)

inv<-c(5,4,3,2,1);
LIdur <- cbind(dur[,2],dur[,4],dur[,6],dur[,8]);
LIapply <- cbind(apply[,2],apply[,4],apply[,6],apply[,8]);

MTTR <- LIdur + LIapply - 1; #1 seconds due to the rename action
MTBF = 3600; #60 minutes = 3600 seconds

#These coefficient denotes the % of applications impacted
#by the load increase
#print(altered)
MTTR <- MTTR[inv,];
#print(MTTR)
for (i in 1:4) {
    MTTR[,i] <- MTTR[,i] * (altered[i,2]/100);
}
av <- MTBF / (MTBF + MTTR) * 100;


partSize <- c(250,500,1000,2500,5000)/1000;
consRatio <- c(15,20,25,30);
#print(av);
filled.contour(consRatio,partSize,t(av),
      ylim=c(250,5000)/1000,
      xlim=c(15,30),
      zlim=c(99,100),
      xlab="",
      ylab="Partition size (x 1000 servers)",
      plot.axes = {axis(2,seq(1,5,by=1))	
                   axis(1,consRatio,las=1)},
      col=gray(seq(0.1,0.9,length=10)),
      nlevels=10
);

mtext("Virtual machines (x 1000)          ",line=-14);
mtext("Availability (%)", line=-6,side=4);
dev.off()