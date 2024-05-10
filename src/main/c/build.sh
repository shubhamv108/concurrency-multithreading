#!/bin/bash

# Loop through all .c files in the current directory
for file in *.c; do
    # Get the base name of the file (without the .c extension)
    base_name="${file%.c}"
    
    # Compile the .c file into an executable with the same base name
    gcc "$file" -o "$base_name"
    
    # Check if compilation succeeded
    if [ $? -eq 0 ]; then
        echo "Compiled $file -> $base_name"
    else
        echo "Failed to compile $file"
    fi
done

