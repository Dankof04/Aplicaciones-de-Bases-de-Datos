#!/bin/bash

# Verifica si se proporcionaron los argumentos correctos
if [ "$#" -ne 1 ]; then
    echo "Uso: $0 <mensaje>"
    exit 1
fi

# Asigna los argumentos a variables
mensaje=$1


# Agrega el archivo al área de preparación
git add workspace

# Realiza el commit con el mensaje proporcionado
git commit -m "$mensaje"

# Empuja los cambios al repositorio remoto
git push

echo "Commit realizado"

