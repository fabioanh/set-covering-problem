#!/bin/bash

#USAGE: ./test-ants.sh instance folder

#Fixed Values
jar_location="../target/set-covering-problem-1.0-SNAPSHOT-jar-with-dependencies.jar"
instances_dir="scp-instances/"
output_directory="output"
output_filename=${output_directory}"/out_"
instances_file="instances.txt"

#initial set up
rm ${output_filename}*
rm -r ${output_directory}
mkdir ${output_directory}

#inputs
redundancy_elim=$1
chs_in=$2
seed_num=$3


if ${redundancy_elim}; then
	output_filename=${output_filename}re_
fi

#Chosen constructive heuristics
IFS=',' read -ra chs <<< "$chs_in"

i=0
while read -ra F  ; do
	#echo ${F[1]}
	instances[${i}]=${F}
	i=$(expr $i + 1)
done < ${instances_file}

#Standard deviation calculation
for ch in "${chs[@]}"; do
	ch_profit_re=""
	ch_time=""
	ch_cost=""
	for inst in "${instances[@]}"; do
		IFS=';' read -ra instance <<< "$inst"
		echo ${ch}-${instance[0]}
		for (( seed=1; seed<=$seed_num; seed++ )); do
			if ${redundancy_elim}; then
				output="$(java -jar ${jar_location} -instance ${instances_dir}${instance[0]} -seed ${seed} -ch ${ch} -re)"
				ch_profit_re+=$(printf "%s\n" $(echo ${output} | grep -o -E 'RedEl profit value: [0-9]+' | cut -d ' ' -f4))$' '
				#echo "${output}" | tee -a ${output_filename}${ch} >> ${output_filename}${ch}_${instance[0]}
				#java -jar ${jar_location} -instance ${instances_dir}${instance[0]} -seed ${seed} -ch ${ch} -re | tee -a ${output_filename}${ch} >> ${output_filename}${ch}_${instance[0]}
			else
				output="$(java -jar ${jar_location} -instance ${instances_dir}${instance[0]} -seed ${seed} -ch ${ch})"
				#echo "${output}" | tee -a ${output_filename}${ch} >> ${output_filename}${ch}_${instance[0]}
				#java -jar ${jar_location} -instance ${instances_dir}${instance[0]} -seed ${seed} -ch ${ch} | tee -a ${output_filename}${ch} >> ${output_filename}${ch}_${instance[0]}
			fi
			ch_time+=$(printf "%s\n" $(echo ${output} | grep -o -E 'Exec Time: [0-9]+' | cut -d ' ' -f3))$' '
			ch_cost+=$(printf "%s\n" $(echo ${output} | grep -o -E 'Total cost: [0-9]+' | cut -d ' ' -f3))$' '
			cost=$(echo ${output} | grep -o -E 'Total cost: [0-9]+' | cut -d ' ' -f3)
			best=${instance[1]}
			echo $(awk -v cost=$cost -v best=$best 'BEGIN { print (cost - best) / best }') >> ${output_filename}${ch}_percent_deviation
		done
		#echo "Best: "${instance[1]} >> ${output_filename}${ch}_${instance[0]}
		#echo $(cat ${output_filename}${ch}_${instance[0]} | grep -o -E 'Total cost: [0-9]+' | cut -d ' ' -f3) >> ${output_filename}${ch}_${instance[0]}
	done
	if ${redundancy_elim}; then
		echo ${ch_profit_re} | tr " " "\n" >> ${output_filename}${ch}_re_profit
	fi
	echo ${ch_time} | tr " " "\n" >> ${output_filename}${ch}_times
	echo ${ch_time} | tr " " "\n" >> ${output_filename}total_times
	echo ${ch_cost} | tr " " "\n" >> ${output_filename}${ch}_costs
	echo ${ch_cost} | tr " " "\n" >> ${output_filename}total_costs
done

#for ch in "${chs[@]}"; do
	#Extract costs
#	printf "%s\n" $(cat ${output_filename}${ch} | grep -o -E 'Total cost: [0-9]+' | cut -d ' ' -f3) > ${output_filename}${ch}_costs
	#Extract times
#	printf "%s\n" $(cat ${output_filename}${ch} | grep -o -E 'Exec Time: [0-9]+' | cut -d ' ' -f3) > ${output_filename}${ch}_times
#	if ${redundancy_elim}; then
		#Extract Redundancy Elimination Profit
#		printf "%s\n" $(cat ${output_filename}${ch} | grep -o -E 'RedEl profit value: [0-9]+' | cut -d ' ' -f4) > ${output_filename}${ch}_re_profit
#	fi
#done

