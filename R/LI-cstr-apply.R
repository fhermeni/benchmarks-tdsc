#!/usr/bin/Rscript
require(Hmisc);
args <- commandArgs(TRUE);
input <- args[1];
output <- args[2];
pdf(paste(output,'/LI-cstrs-apply.pdf',sep=""), height=2, width=5)
# Trim off excess margin space (bottom, left, top, right)
par(mar=c(3.1, 3.9, 0.2, 0.5),ps=15,mgp=c(2.5,0.6,0))

dur <- read.table(paste(input,"/constraints-apply.data",sep=""),sep='\t',header=T)

sizes <- dur[,1];
plot(1,type="n",axes=F,xlim=c(15,30),ylim=c(-2,8),xlab="",ylab="",tck=0.2)
abline(h=0,col="black",lty=2,lwd=1)


pchs <- c(1,2,3)
colors = gray(seq(0.8,0.0,length=3));
lwds <- c(3,3,3)
for (i in 3:5) {
    lines(sizes, dur[,i] - dur[,2], lwd=lwds[i-2], type="o", pch=pchs[i-2], col=colors[i-2]);
}

title(ylab="Time (sec)")
mtext("Virtual machines (x 1,000)",line=-9.5);

axis(1,seq(15,30,by=5))
axis(2,seq(-2,8,by=2),las=1)

legend("topleft",c("33%","66%","100%"),col=colors,lwd=lwds,bty="n",pch=pchs,horiz=TRUE)
dev.off()