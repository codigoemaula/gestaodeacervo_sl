# Sala de Leitura 1.2 — preparação de distribuição e proteção de dados

Esta documentação descreve controles técnicos; não certifica conformidade legal de cada unidade. O aplicativo destina-se à operação por professores e outros educadores responsáveis, não à interação direta por estudantes.

## Funcionamento do educador

- Cadastro, empréstimos, devoluções e relatórios funcionam sem conta online e sem internet.
- Desbloqueio do aparelho Android no início e somente depois de cinco minutos em segundo plano. Configure PIN/senha segura no próprio aparelho; sem proteção, os dados pessoais não são exibidos.
- ISBN e atualização das faixas ISBN são as únicas consultas externas previstas; fornecedores recebem dados técnicos usuais da conexão, inclusive IP.
- Fotos opcionais são escolhidas da galeria, redimensionadas e guardadas privadamente. Não há reconhecimento facial.
- O educador pode remover os dados de leitores com todos os empréstimos devolvidos. Cadastros sem histórico são excluídos; com histórico, nome, identificador, turma, função, foto e código identificador são removidos e o histórico permanece agregado sob pseudônimo. Cópias exportadas previamente exigem tratamento separado.
- CSV é texto legível e requer confirmação. O formato de backup 2 usa senha do educador, PBKDF2 e AES-256-GCM autenticado e inclui banco e fotografias. Senhas não são armazenadas, não há recuperação online.

## Migração e dados anteriores — atenção obrigatória

O aplicativo anterior usa identificador de pacote diferente e builds de depuração podem ter assinaturas diferentes. A edição independente `com.codigoemaula.salaleitura` será vista pelo Android como OUTRO aplicativo, não atualização. Não desinstale a instalação antiga. Faça backup antigo, importe na versão nova utilizando a opção legada, confira os registros com dados fictícios/controle e só então migre de fato. O backup legado contém banco mas NÃO inclui fotos; as fotografias devem ser recuperadas manualmente da instalação anterior antes de desinstalá-la. Criar novo backup criptografado após a migração. Não prometer migração integral automática de fotografias antigas.

## Assinatura estável de produção (responsabilidade do mantenedor)

O repositório é público; NUNCA adicione ou envie chaves `.jks`, senhas, arquivos de dados ou backups ao Git. O fluxo `.github/workflows/production-release.yml` somente produz APK assinado se existirem os seguintes segredos do repositório, configurados pelo proprietário em **Settings > Secrets and variables > Actions > New repository secret**:

- `SALA_RELEASE_KEYSTORE_B64`: conteúdo base64 de uma chave privada de assinatura Android, criada e guardada pelo proprietário fora do repositório.
- `SALA_RELEASE_STORE_PASSWORD`: senha do keystore.
- `SALA_RELEASE_KEY_ALIAS`: nome do alias da chave.
- `SALA_RELEASE_KEY_PASSWORD`: senha da chave.

Para preparar a chave em máquina confiável, utilize Android Studio (`Build > Generate Signed Bundle / APK > Create new`) ou `keytool` com parâmetros de segurança adequados. Guarde backups da chave e de suas senhas fora do GitHub e não compartilhe estas informações com o assistente. A assinatura DEVE ser reutilizada em cada atualização do mesmo `applicationId`. O fluxo falha explicitamente se faltar qualquer segredo e não publica APK de produção sem assinatura verificada. **O arquivo do workflow precisa primeiro integrar a branch padrão do repositório**, após revisão dos testes e do código, para aparecer como execução manual na aba Actions. Depois, execute `Sala de Leitura - Release Segura` em **Actions > Run workflow** e selecione a branch aprovada. O artefato assinado é emitido somente após testes automatizados, build e verificação de assinatura. Uma versão *debug* NÃO é produção.

## Checklist de implantação com dados reais

1. Nomear e documentar o controlador (escola/organização), canal de contato, hipótese legal, finalidade, necessidade e acesso autorizado; definir prazo de conservação e procedimentos de incidentes e atendimento aos titulares, observando LGPD, melhor interesse e proteção de crianças e adolescentes.
2. Testar em aparelho Android real: abertura por credencial, retomar antes/depois de cinco minutos, cadastro, ISBN offline e online, livros, empréstimos e devoluções, remoção com e sem empréstimos pendentes.
3. Testar backup com banco e fotos, senha incorreta e arquivo adulterado, restauração a partir de versão anterior, interoperabilidade e verificação dos registros após reiniciar. Não desinstalar aplicativo anterior antes desse teste.
4. Verificar tráfego de rede e permissões no APK FINAL assinado, ausência de envio de nomes, turmas, fotos, acervo e circulação. Inspecionar comportamento do sistema de arquivos, compartilhamento CSV e outros aplicativos instalados no dispositivo.
5. Verificar políticas institucionais de guarda do aparelho, compartilhamento e eliminação de CSVs/backups, além de revisão jurídica contextual e atualização da documentação da organização.

**Status:** código e pipeline em preparação. Somente a assinatura pelo proprietário, validação em dispositivo e governança da unidade permitem distribuição responsável. A aprovação de CI sozinha não equivale a homologação, nem à certificação de conformidade com a LGPD ou o ECA Digital.
