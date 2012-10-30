#!/usr/bin/Rscript
args <- commandArgs(TRUE);
input <- args[1];
output <- args[2];

pdf(paste(output,'/availability.pdf',sep=""), height=3, width=5);


# Trim off excess margin space (bottom, left, top, right)
par(mar=c(3, 3, 0.5, 0.5),ps=12,mgp=c(1.8,0.6,0))
dur <- read.table(paste(input,"/partitions-duration.data",sep=""),sep='\t',header=T)
apply <- read.table(paste(input,"/partitions-apply.data",sep=""),sep='\t',header=T)

inv<-c(5,4,3,2,1);
LIdur <- cbind(dur[,2],dur[,4],dur[,6],dur[,8]);
LIapply <- cbind(apply[,2],apply[,4],apply[,6],apply[,8]);

MTTR <- LIdur + LIapply - 1; #1 seconds due to the rename action
MTBF = 3600; #60 minutes = 3600 seconds

#Computed using ImpactedApplications.java
#R6 avg. impacted apps: 976.3/1722 = 56.69570267131242%
#R5 avg. impacted apps: 331.92/1420 = 23.374647887323945%
#R4 avg. impacted apps: 67.72/1116 = 6.068100358422939%
#R3 avg. impacted apps: 12.38/852 = 1.4530516431924883%


#These coefficient denotes the % of applications impacted
#by the load increase
MTTR[,1]<-MTTR[,1] * 0.0145;
MTTR[,2]<-MTTR[,2] * 0.061;
MTTR[,3]<-MTTR[,3] * 0.234;
MTTR[,4]<-MTTR[,4] * 0.567;

av <- MTBF / (MTBF + MTTR) * 100;


partSize <- c(250,500,1000,2500,5000);
consRatio <- c(15,20,25,30);

filled.contour(partSize,consRatio,av[inv,],
      xlim=c(250,5000),ylim=c(15,30),zlim=c(99,100),
      xlab="",ylab="Virtual machines (per 1,000)",
      plot.axes = {axis(1,partSize)	
                   axis(2,consRatio,las=1)},
     col=gray(seq(1,0.1,length=10)),
     nlevels=10

);

mtext("Partition size (servers)",line=-14);

dev.off()