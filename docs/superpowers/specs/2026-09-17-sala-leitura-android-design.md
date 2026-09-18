# Especificação de Design — Aplicativo Android Offline para Gestão da Sala de Leitura

Data: 17/09/2026

## 1. Objetivo

Desenvolver um aplicativo Android instalável (APK), com funcionamento offline-first, destinado à gestão do acervo das Salas de Leitura da SME-SP. O aplicativo deverá apoiar POSLs no cadastro e organização de obras, exemplares, estudantes, turmas e profissionais, bem como em empréstimos, devoluções, inventários, baixas e relatórios básicos.

A aplicação não dependerá de Firebase ou de qualquer backend para suas funções essenciais. Conectividade será usada apenas para atualização de referências ISBN e enriquecimento opcional de metadados bibliográficos.

## 2. Plataforma e arquitetura

- Android nativo em Kotlin.
- Interface em Jetpack Compose.
- Persistência local em Room sobre SQLite.
- CameraX para captura de câmera.
- ML Kit Barcode Scanning com modelo incorporado ao APK para leitura offline de EAN-13, EAN-8, UPC, QR Code e formatos compatíveis.
- Arquitetura em camadas, separando UI, domínio, persistência, serviços bibliográficos e importação/exportação.
- Operação offline integral para cadastro, consulta, circulação e inventário.

## 3. Organização por escola

Cada instalação deverá ser vinculada a uma unidade escolar. O cadastro da escola deverá conter, no mínimo:

- nome da unidade;
- código EOL da escola;
- DRE;
- ano letivo vigente;
- identificação da Sala de Leitura;
- prazo padrão de empréstimo: 7 ou 14 dias.

Os dados de uma escola não serão misturados aos de outra instalação ou backup.

## 4. Modelo bibliográfico

A aplicação distinguirá obrigatoriamente obra/edição de exemplar físico.

### 4.1 Obra/Edição

Campos previstos:

- ISBN-10;
- ISBN-13;
- título;
- subtítulo;
- autoria;
- ilustradores/tradutores, quando disponíveis;
- editora;
- edição;
- ano de publicação;
- idioma;
- assunto/categorias;
- número de páginas;
- coleção/série;
- CDD/CDU opcionais;
- capa, quando disponível por fonte externa ou inserida pelo usuário.

### 4.2 Exemplar

Cada cópia física terá registro independente, ainda que compartilhe o mesmo ISBN.

Campos previstos:

- identificador interno único;
- vínculo com obra/edição;
- número sequencial do exemplar;
- localização física;
- situação: disponível, emprestado, danificado, extraviado, em manutenção ou baixado;
- data de cadastro;
- observações;
- QR Code interno opcional.

O cadastro em lote permitirá que um único ISBN gere vários exemplares individuais.

## 5. Identificação bibliográfica e atualização

Ao iniciar o aplicativo:

1. o banco local será aberto imediatamente;
2. o aplicativo verificará a existência de conectividade sem bloquear o uso;
3. quando houver internet, poderá verificar atualizações das faixas ISBN internacionais;
4. a última versão válida permanecerá disponível localmente;
5. falhas de rede não impedirão o funcionamento.

Para enriquecimento bibliográfico, o sistema poderá consultar fontes abertas, priorizando Open Library e usando Google Books como fonte secundária, sempre salvando localmente os metadados obtidos.

Não haverá dependência de scraping de páginas da CBL. A arquitetura deverá permitir futura integração com API oficial, caso seja disponibilizada.

## 6. Cadastro por câmera

Fluxo principal:

1. POSL seleciona “Cadastrar livro”.
2. A câmera lê EAN/ISBN.
3. O sistema normaliza e valida o código.
4. Pesquisa primeiro no banco local.
5. Se inexistente e houver internet, consulta metadados bibliográficos.
6. O usuário revisa/corrige os campos.
7. Informa a quantidade de exemplares.
8. O sistema cria os registros físicos individualizados.
9. Opcionalmente, gera etiquetas com QR Code interno.

Cadastro manual sempre deverá estar disponível para livros sem código de barras ou com código ilegível.

## 7. Pessoas e turmas

### 7.1 Estudantes

Campos mínimos:

- nome;
- identificador institucional/matrícula, opcional;
- turma;
- ano/série;
- turno;
- situação ativo/inativo;
- identificador interno/QR Code.

### 7.2 Profissionais

Campos mínimos:

- nome;
- RF opcional;
- função/tipo;
- situação ativo/inativo;
- identificador interno/QR Code.

Não deverão ser coletados dados pessoais sem necessidade operacional, como CPF, endereço ou telefone.

### 7.3 Importação

O aplicativo permitirá importação de estudantes e profissionais por CSV, com pré-visualização e validação antes da gravação.

## 8. Empréstimos

O prazo permitido será de 7 ou 14 dias.

A escola definirá um prazo padrão nas configurações. No momento de cada empréstimo, o POSL poderá alternar rapidamente entre 7 e 14 dias antes da confirmação.

Regras:

- a data prevista de devolução será calculada automaticamente a partir da data do empréstimo;
- o prazo efetivamente escolhido será gravado no empréstimo e não será alterado caso o padrão da escola seja modificado depois;
- exemplares não disponíveis não poderão ser emprestados;
- pessoas inativas não poderão receber novos empréstimos;
- o sistema registrará data/hora, pessoa, exemplar e prazo aplicado;
- empréstimos vencidos serão destacados localmente;
- será possível renovar um empréstimo, aplicando novo prazo de 7 ou 14 dias a partir da data da renovação, preservando o histórico.

Fluxo rápido:

1. escanear pessoa ou pesquisá-la;
2. escanear exemplar;
3. visualizar prazo padrão e opção 7/14;
4. confirmar;
5. registrar transação e data prevista.

## 9. Devoluções

A devolução deverá poder ser feita apenas escaneando o exemplar.

O aplicativo localizará automaticamente o empréstimo ativo, exibirá pessoa e datas, permitirá registrar condição do exemplar e concluirá a devolução. O exemplar retornará ao status disponível, salvo quando marcado como danificado, em manutenção ou baixado.

## 10. Inventário

O aplicativo terá sessões de inventário independentes.

Durante uma sessão, o POSL poderá escanear exemplares sequencialmente. O sistema deverá comparar o conjunto localizado com o cadastro e apresentar, ao menos:

- total cadastrado;
- total localizado;
- total emprestado;
- total não localizado;
- total danificado;
- total baixado.

O encerramento de um inventário não deverá excluir o histórico de sessões anteriores.

## 11. Estados de circulação e conservação

Estados mínimos de exemplar:

- disponível;
- emprestado;
- danificado;
- extraviado;
- em manutenção;
- baixado.

A aplicação deverá manter histórico das alterações relevantes em AuditLog.

## 12. Estrutura de dados principal

Entidades previstas:

- School;
- ClassGroup;
- Person;
- BookEdition;
- BookCopy;
- Loan;
- LoanRenewal;
- InventorySession;
- InventoryItem;
- MetadataCache;
- IsbnRangeData;
- SyncStatus;
- AuditLog;
- AppSettings.

## 13. Telas principais

- Configuração inicial da escola;
- Início/Dashboard;
- Acervo;
- Detalhes da obra;
- Detalhes do exemplar;
- Escanear;
- Cadastro de livro;
- Pessoas;
- Turmas;
- Empréstimos;
- Devoluções;
- Pendências;
- Inventário;
- Relatórios;
- Etiquetas/QR Codes;
- Backup e restauração;
- Configurações.

## 14. Dashboard

Indicadores básicos:

- número de obras;
- número de exemplares;
- exemplares disponíveis;
- empréstimos ativos;
- empréstimos vencidos;
- devoluções do dia;
- exemplares danificados/extraviados;
- último inventário;
- status da base ISBN/metadados.

## 15. Backup e restauração

O aplicativo deverá exportar um arquivo de backup contendo banco local, configurações e metadados necessários à restauração.

Formato lógico sugerido: `.slbackup`.

Exemplo de nome:

`EMEF-NOME-SALALEITURA-2026-09-17.slbackup`

A seleção do destino será feita por meio do seletor de documentos do Android. O aplicativo não dependerá de Google Drive, mas o usuário poderá escolher um provedor disponível no dispositivo, inclusive Drive.

A restauração deverá validar versão e integridade antes de substituir o banco ativo.

## 16. Privacidade e segurança

- Princípio de minimização de dados pessoais.
- Dados operacionais permanecem no dispositivo, salvo exportação explícita do usuário.
- Nenhum login em serviço externo será necessário na versão inicial.
- Backup não será enviado automaticamente à nuvem.
- Operações críticas de restauração e limpeza exigirão confirmação explícita.
- O banco deverá possuir estratégia de migração de esquema para futuras versões.

## 17. Requisitos offline

Sem conexão à internet, deverão continuar funcionando integralmente:

- abertura do aplicativo;
- pesquisa de acervo;
- cadastro manual;
- leitura de códigos de barras;
- cadastro de exemplares;
- cadastro e pesquisa de pessoas/turmas;
- empréstimos;
- devoluções;
- renovações;
- inventários;
- relatórios locais;
- geração de etiquetas/QR Codes;
- backup local.

Somente atualização de referências ISBN e consulta externa de metadados dependerão de conexão.

## 18. Critérios de aceite da primeira versão

A V1 será considerada funcional quando for possível, em um aparelho Android sem internet:

1. configurar uma escola;
2. cadastrar turmas e pessoas;
3. cadastrar uma obra manualmente ou por leitura de ISBN;
4. gerar múltiplos exemplares de uma mesma edição;
5. identificar cada exemplar individualmente;
6. emprestar um exemplar por 7 ou 14 dias;
7. renovar por 7 ou 14 dias;
8. devolver por leitura do exemplar;
9. consultar pendências;
10. executar um inventário;
11. exportar e restaurar backup;
12. fechar e reabrir o aplicativo preservando todos os dados.

## 19. Fora do escopo da V1

- sincronização automática entre várias escolas;
- portal web central da SME-SP;
- integração direta com SGP/EOL;
- reserva online por estudantes;
- autenticação institucional remota;
- backend Firebase;
- dependência obrigatória de nuvem.

A arquitetura deverá, entretanto, evitar decisões que impeçam a inclusão futura desses recursos.
