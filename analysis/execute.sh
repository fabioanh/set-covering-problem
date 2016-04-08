#!/bin/bash

#USAGE: ./test-ants.sh instance folder

#Fixed Values
jar_location="../target/set-covering-problem-1.0-SNAPSHOT-jar-with-dependencies.jar"
instances_dir="scp-instances/"
output_directory="output"
output_filename=${output_directory}"/out_"
instances_file="instances.txt"

#inputs
redundancy_elim=$1
chs_in=$2
seed_num=$3
improvement=$4


if ${redundancy_elim} && [ -z "$improvement" ]; then
	output_filename=${output_filename}re_
fi

if ${redundancy_elim} && ! [ -z "$improvement" ]; then
	output_filename=${output_filename}re_${improvement}_
fi

if ! ${redundancy_elim} && ! [ -z "$improvement" ]; then
	output_filename=${output_filename}${improvement}_
fi


#initial set up
rm ${output_filename}*
#rm -r ${output_directory}
#mkdir ${output_directory}

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
	ch_profit_improv=""
	ch_time=""
	ch_cost=""
	for inst in "${instances[@]}"; do
		IFS=';' read -ra instance <<< "$inst"
		echo ${ch}-${instance[0]}
		for (( seed=1; seed<=$seed_num; seed++ )); do
			if ${redundancy_elim}; then
				if [ -z "$improvement" ] ; then
					output="$(java -jar ${jar_location} -instance ${instances_dir}${instance[0]} -seed ${seed} -ch ${ch} -re)"
					ch_profit_re+=$(printf "%s\n" $(echo ${output} | grep -o -E 'RedEl profit value: [0-9]+' | cut -d ' ' -f4))$' '
				else
					output="$(java -jar ${jar_location} -instance ${instances_dir}${instance[0]} -seed ${seed} -ch ${ch} -re -improvement ${improvement})"
					ch_profit_improv+=$(printf "%s\n" $(echo ${output} | grep -o -E 'Improvement profit value: [0-9]+' | cut -d ' ' -f4))$' '
				fi
			else
				if [ -z "$improvement" ] ; then
					output="$(java -jar ${jar_location} -instance ${instances_dir}${instance[0]} -seed ${seed} -ch ${ch})"
				else
					output="$(java -jar ${jar_location} -instance ${instances_dir}${instance[0]} -seed ${seed} -ch ${ch} -improvement ${improvement})"
					ch_profit_improv+=$(printf "%s\n" $(echo ${output} | grep -o -E 'Improvement profit value: [0-9]+' | cut -d ' ' -f4))$' '
				fi
			fi
			ch_time+=$(printf "%s\n" $(echo ${output} | grep -o -E 'Exec Time: [0-9]+' | cut -d ' ' -f3))$' '
			ch_cost+=$(printf "%s\n" $(echo ${output} | grep -o -E 'Total cost: [0-9]+' | cut -d ' ' -f3))$' '
			cost=$(echo ${output} | grep -o -E 'Total cost: [0-9]+' | cut -d ' ' -f3)
			best=${instance[1]}
			echo $(awk -v cost=$cost -v best=$best 'BEGIN { print (cost - best) / best }') >> ${output_filename}${ch}_percent_deviation
		done
	done
	if ${redundancy_elim} && [ -z "$improvement" ]; then
		echo ${ch_profit_re} | tr " " "\n" >> ${output_filename}${ch}_re_profit
	fi

	if ! [ -z "$improvement" ]; then
		echo ${ch_profit_improv} | tr " " "\n" >> ${output_filename}${ch}_improv_profit
	fi

	echo ${ch_time} | tr " " "\n" >> ${output_filename}${ch}_times
	echo ${ch_time} | tr " " "\n" >> ${output_filename}total_times
	echo ${ch_cost} | tr " " "\n" >> ${output_filename}${ch}_costs
	echo ${ch_cost} | tr " " "\n" >> ${output_filename}total_costs
done
