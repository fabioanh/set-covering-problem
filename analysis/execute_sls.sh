#!/bin/bash

#USAGE: ./test-ants.sh instance folder

#Fixed Values
jar_location="../target/set-covering-problem-1.0-SNAPSHOT-jar-with-dependencies.jar"
instances_dir="scp-instances/"
output_directory="output"
output_filename=${output_directory}"/out_"
instances_file="instances.txt"
beta=4.0
epsilon=0.01
rho=0.8
ants=40
cool=0.012
temp=5


#inputs
sls=$1
seed_num=$2

if [ "$sls" == "sa" ] && ! [ -z "$3" ] ; then
	cool=$3
fi

if [ "$sls" == "sa" ] && ! [ -z "$4" ] ; then
	temp=$4
fi

if [ "$sls" == "aco" ] && ! [ -z "$3" ] ; then
	beta=$3
fi

if [ "$sls" == "aco" ] && ! [ -z "$4" ] ; then
	epsilon=$4
fi

if [ "$sls" == "aco" ] && ! [ -z "$5" ] ; then
	rho=$5
fi

if [ "$sls" == "aco" ] && ! [ -z "$6" ] ; then
	ants=$6
fi

if [ "$sls" == "aco" ] && ! [ -z "$7" ] ; then
	loops=$7
fi


output_filename=${output_filename}sls_${sls}_

#initial set up
rm ${output_filename}*
#rm -r ${output_directory}
#mkdir ${output_directory}


i=0
while read -ra F  ; do
	#echo ${F[1]}
	instances[${i}]=${F}
	i=$(expr $i + 1)
done < ${instances_file}

exec_time=""
total_cost=""
for inst in "${instances[@]}"; do
	IFS=';' read -ra instance <<< "$inst"
	echo ${sls}-${instance[0]}
	for (( seed=1; seed<=$seed_num; seed++ )); do
		if [ -z "$loops" ] ; then
			output="$(java -jar ${jar_location} -instance ${instances_dir}${instance[0]} -seed ${seed} -sls ${sls} -ch ch4 -re -temp ${temp} -cool ${cool} -beta ${beta} -epsilon ${epsilon} -rho ${rho} -ants ${ants} -duration ${instance[2]})"
		else
			output="$(java -jar ${jar_location} -instance ${instances_dir}${instance[0]} -seed ${seed} -sls ${sls} -ch ch4 -re -temp ${temp} -cool ${cool} -beta ${beta} -epsilon ${epsilon} -rho ${rho} -ants ${ants} -duration ${instance[2]} -loops ${instance[3]})"
		fi
		exec_time+=$(printf "%s\n" $(echo ${output} | grep -o -E 'Exec Time: [0-9]+' | cut -d ' ' -f3))$' '
		total_cost+=$(printf "%s\n" $(echo ${output} | grep -o -E 'Total cost: [0-9]+' | cut -d ' ' -f3))$' '
		cost=$(echo ${output} | grep -o -E 'Total cost: [0-9]+' | cut -d ' ' -f3)
		best=${instance[1]}
		echo $(awk -v cost=$cost -v best=$best 'BEGIN { print (cost - best) / best }') >> ${output_filename}_percent_deviation
	done
done

echo ${exec_time} | tr " " "\n" >> ${output_filename}${sls}_times
echo ${exec_time} | tr " " "\n" >> ${output_filename}total_times
echo ${total_cost} | tr " " "\n" >> ${output_filename}${sls}_costs
echo ${total_cost} | tr " " "\n" >> ${output_filename}total_costs