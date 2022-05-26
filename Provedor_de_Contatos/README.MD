# Provedor de contatos



O Provedor de contatos é um componente poderoso e flexível do Android que gerencia o principal repositório de dados sobre pessoas do dispositivo. O Provedor de contatos é a fonte dos dados vistos nos aplicativos de contatos do dispositivo e, por ele, também é possível acessar os dados no próprio aplicativo e transferi-los entre o dispositivo e serviços on-line. O provedor fornece uma grande variedade de fontes de dados e tenta gerenciar o máximo de dados possíveis para cada pessoa, uma vez que organizá-los é algo complexo. Por isso, a API do provedor contém um conjunto extensivo de classes e interfaces de contrato que facilitam a recuperação e a modificação dos dados.

Este guia descreve o seguinte:

- A estrutura básica do provedor.
- Como recuperar dados por um provedor.
- Como modificar dados no provedor.
- Como criar um adaptador de sincronização para sincronizar dados do servidor com o Provedor de contatos.

Este guia considera que o leitor conhece os fundamentos sobre provedores de conteúdo do Android. Para saber mais sobre provedores de conteúdo do Android, leia o guia [Fundamentos do provedor de conteúdo](https://developer.android.com/guide/topics/providers/content-provider-basics).

## Organização do Provedor de contatos

O Provedor de contatos é um componente do provedor de conteúdo do Android. Ele mantém três tipos de dados sobre uma pessoa, sendo cada um deles correspondente a uma tabela fornecida pelo provedor, como ilustrado na figura 1:

![img](https://developer.android.com/images/providers/contacts_structure.png)

**Imagem 1.** Estrutura da tabela do Provedor de contatos.

As três tabelas são comumente identificadas pelo nome de suas classes de contrato. As classes definem constantes para URIs de conteúdo e nomes e valores de colunas usados pela tabela:

- Tabela `ContactsContract.Contacts`

  As linhas representam pessoas diferentes com base em agregações de linhas de contatos brutos.

- Tabela `ContactsContract.RawContacts`

  As linhas contêm um resumo dos dados de uma pessoa, específicos a um tipo e uma conta de usuário.

- Tabela `ContactsContract.Data`

  As linhas contêm os detalhes dos contatos brutos, como endereços de e-mail ou números de telefone.

As outras tabelas representadas pelas classes de contrato em `ContactsContract` são tabelas auxiliares que o Provedor de contatos usa para gerenciar suas operações ou compatibilizar funções específicas nos contatos do dispositivo ou em aplicativos de telefonia.

## Contatos brutos

Os contatos brutos representam os dados de uma pessoa provenientes de um tipo único de conta e um nome de conta. Como o Provedor de contatos permite mais de um serviço on-line como fonte de dados de uma pessoa, ele permite diversos contatos brutos para a mesma pessoa. Diversos contatos brutos também permitem que um usuário combine os dados de uma pessoa de mais de uma conta com o mesmo tipo de conta.

A maioria dos dados de um contato bruto não é armazenada na tabela `ContactsContract.RawContacts`. Em vez disso, é armazenada em uma ou mais linhas na tabela `ContactsContract.Data`. Cada linha de dados tem uma coluna `Data.RAW_CONTACT_ID` que contém o valor `RawContacts._ID` de sua linha `ContactsContract.RawContacts` pai.

### Colunas importantes de contatos brutos

As colunas importantes da tabela `ContactsContract.RawContacts` estão listadas na tabela 1. Leia as observações que se seguem após a tabela:

**Tabela 1.** Importantes colunas de contatos brutos.

| Nome da coluna | Uso                                                          | Observações                                                  |
| :------------- | :----------------------------------------------------------- | :----------------------------------------------------------- |
| `ACCOUNT_NAME` | É o nome da conta para o tipo de conta que é a fonte desse contato bruto. Por exemplo, o nome da conta de uma Conta do Google é um dos endereços do Gmail do proprietário do dispositivo. Para mais informações, acesse o próximo item `ACCOUNT_TYPE`. | O formato desse nome é específico deste tipo de conta. Não se trata necessariamente de um endereço de e-mail. |
| `ACCOUNT_TYPE` | É o tipo de conta que é a fonte desse contato bruto. Por exemplo, o tipo de conta de uma Conta do Google é `com.google`. Sempre qualifique o tipo de conta com um identificador de domínio para um domínio que você controla ou tem. Isso garantirá que seu tipo de conta seja exclusivo. | Os tipos de conta que fornecem dados de contatos normalmente têm um adaptador de sincronização associado que se sincroniza com o Provedor de contatos. |
| `DELETED`      | É o sinalizador “excluído” de um contato bruto.              | Esse sinalizador permite ao Provedor de contatos manter a linha internamente até os adaptadores de sincronização poderem excluí-la dos servidores e, em seguida, excluí-la do repositório. |

#### Observações

As observações a seguir sobre a tabela `ContactsContract.RawContacts` são importantes:

- O nome de um contato bruto não é armazenado na sua linha em `ContactsContract.RawContacts`. Em vez disso, é armazenado na tabela `ContactsContract.Data`, em uma linha `ContactsContract.CommonDataKinds.StructuredName`. Os contatos brutos têm somente uma linha desse tipo na tabela `ContactsContract.Data`.

- Atenção:

   

  para usar os dados da própria conta em uma linha de contato bruto, é necessário registrá-la primeiro com o

   

  ```
  AccountManager
  ```

  . Para isso, faça com que os usuários adicionem o tipo e o nome da conta à lista de contas. Caso contrário, o Provedor de contatos excluirá a linha do contato bruto automaticamente.

  Por exemplo: se você quiser que o aplicativo mantenha dados de contato do seu serviço baseado em Web com o domínio `com.example.dataservice` e a conta do usuário do serviço for `becky.sharp@dataservice.example.com`, o usuário precisará, primeiramente, adicionar o “tipo” de conta (`com.example.dataservice`) e o “nome” da conta (`becky.smart@dataservice.example.com`) antes que o aplicativo adicione linhas de contato bruto. Você pode explicar esse requisito ao usuário em documentações ou pode exigir que o usuário adicione o tipo, o nome ou ambos. Tipos e nomes de conta são descritos com mais detalhes na próxima seção.

### Fontes de dados de contatos brutos

Para compreender como os contatos brutos funcionam, considere a usuária “Emily Dickinson”, que tem as seguintes três contas de usuário definidas no dispositivo:

- `emily.dickinson@gmail.com`
- `emilyd@gmail.com`
- Conta do Twitter “belle_of_amherst”

Essa usuária ativou *Sync Contacts* para todas as três contas nas configurações de *Accounts*.

Suponhamos que a Emily Dickinson abra uma janela do navegador, acesse o Gmail como `emily.dickinson@gmail.com`, abra “Contatos” e adicione “Thomas Higginson”. Mais tarde, ela acessa o Gmail como `emilyd@gmail.com` e envia um e-mail para “Thomas Higginson”, o que automaticamente o adiciona como um contato. Ela também segue “colonel_tom” (código do Twitter de Thomas Higginson) no Twitter.

O Provedor de contatos cria três contatos brutos como resultado desse trabalho:

1. Um contato bruto de "Thomas Higginson" associado a `emily.dickinson@gmail.com`. O tipo de conta do usuário é Google.
2. Um segundo contato bruto de "Thomas Higginson" associado a `emilyd@gmail.com`. O tipo da conta de usuário também é do Google. Há um segundo contato bruto, embora o nome seja idêntico a um nome anterior, porque a pessoa foi adicionada a uma conta de usuário diferente.
3. E um terceiro contato bruto de “Thomas Higginson” associado a “belle_of_amherst”. O tipo de conta de usuário é do Twitter.

## Dados

Como observado anteriormente, os dados de um contato bruto são armazenados em uma linha `ContactsContract.Data` vinculada ao valor `_ID` do contato bruto. Isso permite que um único contato bruto tenha diversas instâncias do mesmo tipo de dados, como endereços de e-mail ou números de telefone. Por exemplo: se “Thomas Higginson” para `emilyd@gmail.com` (a linha do contato bruto de Thomas Higginson associada à Conta do Google `emilyd@gmail.com`) tiver um endereço de e-mail pessoal `thigg@gmail.com` e um profissional `thomas.higginson@gmail.com`, o Provedor de contatos armazenará as duas linhas de endereço de e-mail e vinculará ambas ao contato bruto.

Diferentes tipos de dados são armazenados nessa única tabela. Linhas de nome de exibição, número de telefone, e-mail, endereço postal, foto e detalhes de site são encontradas na tabela `ContactsContract.Data`. Para ajudar a gerenciar isso, a tabela `ContactsContract.Data` tem algumas colunas com nomes descritivos e outras com nomes genéricos. O conteúdo de uma coluna de nome descritivo tem o mesmo significado independentemente do tipo de dado da linha, enquanto o conteúdo de uma coluna de nome genérico tem diferentes significados dependendo do tipo de dados.

### Nomes descritivos de colunas

Veja alguns exemplos de nomes descritivos de colunas:

- `RAW_CONTACT_ID`

  É o valor da coluna `_ID` do contato bruto para esses dados.

- `MIMETYPE`

  É o tipo de dado armazenado nessa linha, expresso como um tipo MIME personalizado. O Provedor de contatos usa os tipos MIME definidos nas subclasses de `ContactsContract.CommonDataKinds`. Esses tipos MIME têm código aberto e podem ser usados por qualquer aplicativo ou adaptador de sincronização que funcione com o Provedor de contatos.

- `IS_PRIMARY`

  Se esse tipo de linha de dados puder ocorrer mais do que uma vez em um contato bruto, a coluna `IS_PRIMARY` sinalizará a linha de dados que contém os dados primários do tipo. Por exemplo, se o usuário pressionar por algum tempo um número de telefone de um contato e selecionar **Definir padrão**, a linha `ContactsContract.Data` que contém esse número terá a coluna `IS_PRIMARY` definida como um valor diferente de zero.

### Nomes de colunas genéricas

Há 15 colunas genéricas de nome `DATA1` a `DATA15` que estão disponíveis de forma geral e quatro colunas genéricas adicionais, `SYNC1` a `SYNC4`, que devem ser usadas somente por adaptadores de sincronização. As constantes de nome de coluna genérica sempre funcionam independentemente do tipo de dados que a linha contenha.

A coluna `DATA1` é indexada. O Provedor de contatos sempre usa essa coluna para os dados que o provedor espera serem os alvos mais frequentes de uma consulta. Por exemplo, em uma linha de e-mail, essa coluna contém o endereço de e-mail atual.

Por convenção, a coluna `DATA15` é reservada para armazenar dados de BLOBs (Binary Large Object), como miniaturas de fotos.

### Nomes de coluna de tipo específico

Para facilitar o trabalho com as colunas para um tipo específico de linha, o Provedor de contatos também fornece constantes de nome de colunas de tipo específico, definidas em subclasses de `ContactsContract.CommonDataKinds`. As constantes simplesmente dão um nome de constante diferente para o mesmo nome de coluna, o que ajuda no acesso aos dados em uma linha de um tipo específico.

Por exemplo, a classe `ContactsContract.CommonDataKinds.Email` define constantes de nome de coluna de tipo específico para uma linha `ContactsContract.Data` que tem o tipo MIME `Email.CONTENT_ITEM_TYPE`. A classe contém a constante `ADDRESS` para a coluna de endereço de e-mail. O valor atual de `ADDRESS` é “data1”, que é igual ao nome genérico da coluna.

**Atenção:** não adicione seus dados personalizados à tabela `ContactsContract.Data` usando uma linha que tenha um dos tipos MIME predefinidos do provedor. Caso contrário, há o risco de perda de dados ou mau funcionamento do provedor. Por exemplo, não se deve adicionar uma linha com o tipo MIME `Email.CONTENT_ITEM_TYPE` que contenha um nome de usuário em vez de um endereço de e-mail na coluna `DATA1`. Se for usado um tipo MIME personalizado para a linha, será possível definir os próprios nomes de coluna de tipo específico e usar as colunas como quiser.

A figura 2 mostra como colunas descritivas e colunas de dados aparecem em uma linha `ContactsContract.Data` e como os nomes de coluna de tipo específico se “sobrepõem” aos nomes de coluna genérica.

![Como nomes de coluna de tipo específico mapeiam nomes de coluna genéricos](https://developer.android.com/images/providers/data_columns.png)

**Imagem 2.** Nomes de coluna de tipo específico e nomes de coluna genérica.

### Classes de nome de coluna de tipo específico

A tabela 2 lista as classes de nome de coluna de tipo específico mais usadas:

**Tabela 2.** Classes de nome de coluna de tipo específico

| Classes de mapeamento                               | Tipo de dados                                                | Observações                                                  |
| :-------------------------------------------------- | :----------------------------------------------------------- | :----------------------------------------------------------- |
| `ContactsContract.CommonDataKinds.StructuredName`   | São os dados de nome do contato bruto associados a essa linha de dados. | Os contatos brutos têm somente uma dessas linhas.            |
| `ContactsContract.CommonDataKinds.Photo`            | É a foto principal do contato bruto associada a essa linha de dados. | Os contatos brutos têm somente uma dessas linhas.            |
| `ContactsContract.CommonDataKinds.Email`            | É um endereço de e-mail do contato bruto associado a essa linha de dados. | Os contatos brutos podem ter diversos endereços de e-mail.   |
| `ContactsContract.CommonDataKinds.StructuredPostal` | É um endereço postal do contato bruto associado a essa linha de dados. | Os contatos brutos podem ter diversos endereços postais.     |
| `ContactsContract.CommonDataKinds.GroupMembership`  | É um identificador que vincula o contato bruto a um dos grupos no Provedor de contatos. | Grupos são um recurso opcional de um tipo e um nome de conta. Eles são descritos mais detalhadamente na seção [Grupos de contato](https://developer.android.com/guide/topics/providers/contacts-provider#Groups). |

### Contatos

O Provedor de contatos combina as linhas do contato bruto entre todos os tipos e nomes de conta para formar um **contato**. Isso facilita a exibição e modificação de todos os dados de uma pessoa que um usuário tenha coletado. O Provedor de contatos gerencia a criação de linhas de novos contatos e a agregação de contatos brutos a uma linha de contato existente. Nem os aplicativos nem os adaptadores de sincronização têm permissão para adicionar contatos, e algumas colunas em uma linha de contato são somente leitura.

**Observação:** a tentativa de adicionar um contato ao Provedor de contatos com um `insert()` gerará uma exceção `UnsupportedOperationException`. A tentativa de atualizar uma coluna listada como “somente leitura” será ignorada.

O Provedor de contatos cria um novo contato em resposta à adição de um novo contato bruto que não corresponda a nenhum contato existente. O provedor também faz isso se os dados de um contato bruto existente mudam de modo a não corresponder mais ao contato inserido anteriormente. Se um aplicativo ou um adaptador de sincronização criar um novo contato bruto que *corresponda* a um contato existente, o novo contato bruto será agregado ao contato existente.

O Provedor de contatos vincula uma linha do contato às linhas do contato bruto com a coluna `_ID` da linha do contato na tabela `Contacts`. A coluna `CONTACT_ID` da tabela de contatos brutos `ContactsContract.RawContacts` contém valores `_ID` para a linha dos contatos associados a cada linha dos contatos brutos.

A tabela `ContactsContract.Contacts` também tem a coluna `LOOKUP_KEY`, que é um vínculo “permanente” com a linha do contato. Como o Provedor de contatos mantém contatos automaticamente, ele pode mudar o valor de `_ID` da linha do contato em resposta a uma agregação ou sincronização. Mesmo que isso aconteça, o URI de conteúdo `CONTENT_LOOKUP_URI` combinado com `LOOKUP_KEY` do contato continuará apontado para a linha do contato para permitir o uso de `LOOKUP_KEY` e manter ligações com contatos “favoritos” e assim por diante. Essa coluna tem o próprio formato, que não tem nenhuma relação com o formato da coluna `_ID`.

A figura 3 mostra como as três tabelas principais se relacionam entre si.

![Principais tabelas do Provedor de contatos](https://developer.android.com/images/providers/contacts_tables.png)

**Imagem 3.** Contatos, contatos brutos e relacionamentos da tabela Detalhes.

**Atenção:** se você publicar seu aplicativo na Google Play Store ou se ele estiver em um dispositivo que opere no Android 10 (nível de API 29) ou em versões posteriores, é importante lembrar que um conjunto limitado de métodos e campos de dados de contatos estão obsoletos.

Nas condições citadas, o sistema limpa periodicamente qualquer valor registrado nestes campos:

- [`ContactsContract.ContactOptionsColumns.LAST_TIME_CONTACTED`](https://developer.android.com/reference/android/provider/ContactsContract.ContactOptionsColumns#LAST_TIME_CONTACTED)
- [`ContactsContract.ContactOptionsColumns.TIMES_CONTACTED`](https://developer.android.com/reference/android/provider/ContactsContract.ContactOptionsColumns#TIMES_CONTACTED)
- [`ContactsContract.DataUsageStatColumns.LAST_TIME_USED`](https://developer.android.com/reference/android/provider/ContactsContract.DataUsageStatColumns#LAST_TIME_USED)
- [`ContactsContract.DataUsageStatColumns.TIMES_USED`](https://developer.android.com/reference/android/provider/ContactsContract.DataUsageStatColumns#TIMES_USED)

As APIs usadas para definir os campos de dados citados acima também estão obsoletas:

- [`ContactsContract.Contacts.markAsContacted()`](https://developer.android.com/reference/android/provider/ContactsContract.Contacts#markAsContacted(android.content.ContentResolver, long))
- [`ContactsContract.DataUsageFeedback`](https://developer.android.com/reference/android/provider/ContactsContract.DataUsageFeedback)

Além disso, os campos a seguir não retornam mais os contatos frequentes. Alguns desses campos influenciam a classificação de contatos somente quando o contato faz parte de um [tipo de dados](https://developer.android.com/reference/android/provider/ContactsContract.CommonDataKinds) específico.

- [`ContactsContract.Contacts.CONTENT_FREQUENT_URI`](https://developer.android.com/reference/android/provider/ContactsContract.Contacts#CONTENT_FREQUENT_URI)
- [`ContactsContract.Contacts.CONTENT_STREQUENT_URI`](https://developer.android.com/reference/android/provider/ContactsContract.Contacts#CONTENT_STREQUENT_URI)
- [`ContactsContract.Contacts.CONTENT_STREQUENT_FILTER_URI`](https://developer.android.com/reference/android/provider/ContactsContract.Contacts#CONTENT_STREQUENT_FILTER_URI)
- `CONTENT_FILTER_URI` (afeta somente os tipos de dados [E-mail](https://developer.android.com/reference/android/provider/ContactsContract.CommonDataKinds.Email#CONTENT_FILTER_URI), [Telefone](https://developer.android.com/reference/android/provider/ContactsContract.CommonDataKinds.Phone#CONTENT_FILTER_URI), [Chamável](https://developer.android.com/reference/android/provider/ContactsContract.CommonDataKinds.Callable#CONTENT_FILTER_URI) e [Contatáveis](https://developer.android.com/reference/android/provider/ContactsContract.CommonDataKinds.Contactables#CONTENT_FILTER_URI))
- `ENTERPRISE_CONTENT_FILTER_URI` (afeta somente os tipos de dados [E-mail](https://developer.android.com/reference/android/provider/ContactsContract.CommonDataKinds.Email#ENTERPRISE_CONTENT_FILTER_URI), [Telefone](https://developer.android.com/reference/android/provider/ContactsContract.CommonDataKinds.Phone#ENTERPRISE_CONTENT_FILTER_URI) e [Chamável](https://developer.android.com/reference/android/provider/ContactsContract.CommonDataKinds.Callable#ENTERPRISE_CONTENT_FILTER_URI))

Caso seus aplicativos acessem ou atualizem esses campos ou essas APIs, use outros métodos. Por exemplo, é possível atender a certos casos de uso por meio de [provedores de conteúdo particular](https://developer.android.com/guide/topics/providers/content-provider-creating) ou outros dados armazenados no seu aplicativo ou em sistemas back-end.

Para assegurar que a funcionalidade do seu aplicativo não seja afetada pela alteração, é possível limpar esses campos de dados manualmente. Para fazer isso, execute o comando ADB a seguir em um dispositivo com Android 4.1 (API nível 16) ou superior:

```bsh
adb shell content delete \
--uri content://com.android.contacts/contacts/delete_usage
```

## Dados de adaptadores de sincronização

Os usuários inserem dados de contato diretamente no dispositivo, mas os dados também são direcionados ao Provedor de contatos de serviços Web via **adaptadores de sincronização**, que automatizam a transferência de dados entre dispositivo e serviços. Adaptadores de sincronização funcionam em segundo plano, controlados pelo sistema, e chamam métodos `ContentResolver` para gerenciar os dados.

No Android, o serviço Web com que um adaptador de sincronização trabalha é identificado por um tipo de conta. Cada adaptador de sincronização funciona com um tipo de conta, mas é compatível com diversos nomes de conta para o tipo em questão. Tipos e nomes de conta são descritos brevemente na seção [Fontes de dados de contatos brutos](https://developer.android.com/guide/topics/providers/contacts-provider#RawContactsExample). As definições a seguir fornecem mais detalhes e descrevem como o tipo e o nome de conta se relacionam com adaptadores de sincronização e serviços.

- Tipo de conta

  Identifica um serviço em que o usuário tenha armazenado dados. Na maior parte do tempo, o usuário precisa autenticar com o serviço. Por exemplo, Contatos do Google é um tipo de conta, identificado pelo código `google.com`. Esse valor corresponde ao tipo de conta usado pelo `AccountManager`.

- Nome da conta

  Identifica uma conta ou login específico de um tipo de conta. As contas Contatos do Google são idênticas às Contas do Google, que têm um endereço de e-mail como nome da conta. Outros serviços podem usar um nome de usuário com só uma palavra ou código numérico.

Os tipos de conta não precisam ser exclusivos. Um usuário pode configurar diversas contas Contatos do Google e baixar os dados delas para o Provedor de contatos. Isso pode acontecer se o usuário tiver um grupo de contatos pessoais para um nome de conta pessoal e outro grupo para um de conta de trabalho. Nomes de conta normalmente são exclusivos. Juntos, eles identificam um fluxo de dados específicos entre o Provedor de contatos e um serviço externo.

Se você quiser transferir os dados do serviço ao Provedor de contatos, precisará criar o próprio adaptador de sincronização. Isso é descrito com mais detalhes na seção [Adaptadores de sincronização do Provedor de contatos](https://developer.android.com/guide/topics/providers/contacts-provider#SyncAdapters).

A figura 4 mostra como o Provedor de contatos se insere no fluxo de dados sobre pessoas. Na caixa marcada “adaptadores de sincronização”, cada adaptador é rotulado pelo tipo de conta.

![Fluxo de dados sobre pessoas](https://developer.android.com/images/providers/ContactsDataFlow.png)

**Imagem 4.** Fluxo de dados do Provedor de contatos.

## Permissões necessárias

Os aplicativos que queiram acessar o Provedor de contatos precisam solicitar as seguintes permissões:

- Acesso de leitura a uma ou mais tabelas

  `READ_CONTACTS`, especificado em `AndroidManifest.xml` com o elemento `<uses-permission>` como `<uses-permission android:name="android.permission.READ_CONTACTS">`.

- Acesso de gravação a uma ou mais tabelas

  `WRITE_CONTACTS`, especificado em `AndroidManifest.xml` com o elemento `<uses-permission>` como `<uses-permission android:name="android.permission.WRITE_CONTACTS">`.

Essas permissões não se estendem aos dados do perfil do usuário. O perfil do usuário e as permissões necessárias são abordados na seção a seguir, [O perfil do usuário](https://developer.android.com/guide/topics/providers/contacts-provider#UserProfile).

Lembre-se de que os dados de contato do usuário são pessoais e confidenciais. Os usuários se preocupam com a privacidade e, por isso, não querem aplicativos que coletem dados sobre eles ou seus contatos. Se não for óbvio o motivo da necessidade de permissões para acessar os dados de contato de um usuário, eles podem atribuir classificações ruins ao seu aplicativo ou simplesmente não o instalar.

## O perfil do usuário

A tabela `ContactsContract.Contacts` tem uma única linha contendo os dados do perfil do usuário do dispositivo. Esses dados descrevem o `user` do dispositivo em vez de um dos contatos do usuário. A linha de contatos do perfil é vinculada a uma linha de contatos brutos para cada sistema que usa um perfil. Cada linha de contato bruto de perfil pode ter diversas linhas de dados. Constantes de acesso ao perfil do usuário estão disponíveis na classe `ContactsContract.Profile`.

O acesso ao perfil do usuário exige permissões especiais. Além das permissões `READ_CONTACTS` e `WRITE_CONTACTS` necessárias para ler e gravar, o acesso ao perfil do usuário requer as permissões android.Manifest.permission#READ_PROFILE e android.Manifest.permission#WRITE_PROFILE para acesso de leitura e gravação, respectivamente.

Lembre-se de que é preciso considerar a confidencialidade de um perfil do usuário. A permissão android.Manifest.permission#READ_PROFILE permite acessar os dados de identificação pessoal do usuário do dispositivo. Certifique-se de informar ao usuário o motivo da necessidade de permissões de acesso ao perfil do usuário na descrição do aplicativo.

Para recuperar a linha de contato que contém o perfil do usuário, chame `ContentResolver.query()`. Defina o URI de conteúdo como `CONTENT_URI` e não forneça nenhum critério de seleção. Também é possível usar esse URI de conteúdo como base para recuperar contatos brutos ou dados do perfil. Por exemplo, esse snippet recupera dados do perfil:

[Kotlin](https://developer.android.com/guide/topics/providers/contacts-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/contacts-provider#java)

```kotlin
// Sets the columns to retrieve for the user profile
projection = arrayOf(
        ContactsContract.Profile._ID,
        ContactsContract.Profile.DISPLAY_NAME_PRIMARY,
        ContactsContract.Profile.LOOKUP_KEY,
        ContactsContract.Profile.PHOTO_THUMBNAIL_URI
)

// Retrieves the profile from the Contacts Provider
profileCursor = contentResolver.query(
        ContactsContract.Profile.CONTENT_URI,
        projection,
        null,
        null,
        null
)
```

**Observação:** se você recuperar diversas linhas de contato e quiser determinar se uma delas é o perfil do usuário, teste a coluna `IS_USER_PROFILE` da linha. Essa coluna é definida como “1” se o contato for o perfil do usuário.

## Metadados do Provedor de contatos

O Provedor de contatos gerencia dados que acompanham o estado dos dados de contatos no repositório. Esses metadados sobre o repositório são armazenados em vários locais, inclusive os contatos brutos, os dados e as linhas da tabela de contatos, a tabela `ContactsContract.Settings` e a tabela `ContactsContract.SyncState`. A tabela a seguir mostra o efeito de cada uma dessas partes de metadados:

**Tabela 3.** Metadados no Provedor de contatos

| Tabela                                                       | Coluna              | Valores                                                      | Significado                                                  |
| :----------------------------------------------------------- | :------------------ | :----------------------------------------------------------- | :----------------------------------------------------------- |
| `ContactsContract.RawContacts`                               | `DIRTY`             | “0” - sem modificação desde a última sincronização.          | Sinaliza contatos brutos que mudaram no dispositivo e devem ser sincronizados com o servidor. O valor é definido automaticamente pelo Provedor de contatos quando os aplicativos Android atualizam uma linha.Adaptadores de sincronização que modificam o contato bruto ou as tabelas de dados sempre devem anexar a string `CALLER_IS_SYNCADAPTER` ao URI de conteúdo usado. Isso evita que o provedor sinalize alguma linha como suja. Caso contrário, as modificações do adaptador de sincronização parecem ser modificações locais e são enviadas ao servidor, mesmo que o servidor seja a origem da modificação. |
| “1” - modificado desde a última sincronização. Precisa ser sincronizado com o servidor. |                     |                                                              |                                                              |
| `ContactsContract.RawContacts`                               | `VERSION`           | É o número da versão dessa linha.                            | O Provedor de contatos incrementa esse valor automaticamente sempre que a linha ou os dados relacionados a ela mudam. |
| `ContactsContract.Data`                                      | `DATA_VERSION`      | É o número da versão dessa linha.                            | O Provedor de contatos incrementa esse valor automaticamente sempre que a linha de dados é modificada. |
| `ContactsContract.RawContacts`                               | `SOURCE_ID`         | É o valor de uma string que identifica exclusivamente esse contato bruto para a conta em que foi criado. | Quando um adaptador de sincronização cria um novo contato bruto, essa coluna deve ser definida como o código exclusivo do servidor para o contato bruto. Quando um aplicativo Android cria um novo contato bruto, o aplicativo deve deixar essa coluna vazia. Isso sinaliza ao adaptador de sincronização que ele precisa criar um novo contato bruto no servidor e conseguir um valor para o `SOURCE_ID`.Em particular, o código de origem deve ser **exclusivo** de cada tipo de conta e estável em sincronizações:Exclusivo: cada contato bruto de uma conta deve ter o próprio código de origem. Se isso não for aplicado, haverá problemas no aplicativo de contatos. Dois contatos brutos do mesmo *tipo* de conta podem ter o mesmo código de origem. Por exemplo: o contato bruto “Thomas Higginson” da conta `emily.dickinson@gmail.com` pode ter o mesmo código de origem do contato bruto “Thomas Higginson” da conta `emilyd@gmail.com`.Estável: códigos de origem são uma parte permanente dos dados do serviço on-line para o contato bruto. Por exemplo, se o usuário apaga o Armazenamento de contatos nas configurações dos aplicativos e ressincroniza, os contatos brutos restaurados devem ter os mesmos códigos de origem de antes. Se isso não for aplicado, os atalhos deixarão de funcionar. |
| `ContactsContract.Groups`                                    | `GROUP_VISIBLE`     | “0” - os contatos nesse grupo não devem ser visíveis em IUs do aplicativo Android. | Essa coluna se destina à compatibilidade com servidores, que permite que um usuário oculte contatos em determinados grupos. |
| “1” - os contatos nesse grupo podem ser visíveis nas IUs do aplicativo. |                     |                                                              |                                                              |
| `ContactsContract.Settings`                                  | `UNGROUPED_VISIBLE` | “0” - para essa conta e esse tipo de conta, os contatos que não pertencem a um grupo são invisíveis nas IUs do aplicativo Android. | Por padrão, os contatos são invisíveis se nenhum dos contatos brutos pertence a algum grupo (a associação de um contato bruto a grupos é indicada por uma ou mais linhas `ContactsContract.CommonDataKinds.GroupMembership` na tabela `ContactsContract.Data`). É possível fazer com que contatos sem grupos sejam visíveis por meio desse sinalizador na linha da tabela `ContactsContract.Settings` para um tipo de conta e uma conta. Esse sinalizador serve para exibir contatos de servidores que não usam grupos. |
| “1” - para essa conta e esse tipo de conta, os contatos que não pertencem a um grupo são visíveis nas IUs do aplicativo. |                     |                                                              |                                                              |
| `ContactsContract.SyncState`                                 | (todos)             | Use essa tabela para armazenar metadados do seu adaptador de sincronização. | Com essa tabela, é possível armazenar o estado de sincronização e outros dados relacionados à sincronização de forma persistente no dispositivo. |

## Acesso ao Provedor de contatos

Esta seção descreve diretrizes para acessar dados do Provedor de contatos, com foco no seguinte:

- Consultas de entidade.
- Modificação em lote.
- Recuperação e modificação com intents.
- Integridade dos dados.

A realização de modificações de um adaptador de sincronização também é abordada na seção [Adaptadores de sincronização do Provedor de contatos](https://developer.android.com/guide/topics/providers/contacts-provider#SyncAdapters).

Para um exemplo de como usar o Provedor de contatos ao pesquisar por contatos, veja a amostra [Contatáveis básicos](https://github.com/googlesamples/android-BasicContactables).

### Consultas de entidades

A organização hierárquica das tabelas do Provedor de contatos é muito útil para recuperar uma linha e todas as linhas “filhas” vinculadas. Por exemplo, para exibir todas as informações de uma pessoa, você pode querer recuperar todas as linhas `ContactsContract.RawContacts` de uma única linha `ContactsContract.Contacts` ou todas as linhas `ContactsContract.CommonDataKinds.Email` de uma única linha `ContactsContract.RawContacts`. Para facilitar isso, o Provedor de contatos oferece a ideia de **entidade**, que atua como uma junção dos bancos de dados entre tabelas.

Uma entidade é como uma tabela composta de colunas selecionadas de uma tabela pai e uma tabela filha. Ao consultar uma entidade, fornece-se uma projeção e buscam-se critérios com base nas colunas disponíveis da entidade. O resultado é um `Cursor` que contém uma linha para cada linha recuperada da tabela filha. Por exemplo, ao consultar `ContactsContract.Contacts.Entity` de um nome de contato e todas as linhas `ContactsContract.CommonDataKinds.Email` de todos os contatos brutos por esse nome, você terá um `Cursor` contendo uma linha para cada linha `ContactsContract.CommonDataKinds.Email`.

As entidades simplificam as consultas. Ao usar uma entidade, é possível recuperar todos os dados de contatos de um contato ou contato bruto de uma vez, sem precisar consultar primeiro a tabela pai para receber um código e, em seguida, ter que consultar a tabela filha com esse código. Além disso, o Provedor de contatos processa uma consulta em relação a uma entidade em uma transação única, o que garante que os dados recuperados sejam consistentes internamente.

**Observação:** uma entidade normalmente não contém todas as colunas da tabela pai e da tabela filha. Se você tentar trabalhar com um nome de coluna que não esteja na lista de constantes de nomes de coluna da entidade, será gerada uma `Exception`.

O snippet a seguir mostra como recuperar todas as linhas de contato bruto de um contato. O snippet é parte de um aplicativo maior que tem duas atividades: “principal” e “de detalhes”. A atividade principal exibe uma lista de linhas de contato. Quando um usuário seleciona uma delas, a atividade envia o código correspondente à atividade de detalhes. A atividade de detalhes usa `ContactsContract.Contacts.Entity` para exibir todas as linhas de dados de todos os contatos brutos associados ao contato selecionado.

Esse snippet é extraído da atividade “de detalhes”:

[Kotlin](https://developer.android.com/guide/topics/providers/contacts-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/contacts-provider#java)

```kotlin
...
    /*
     * Appends the entity path to the URI. In the case of the Contacts Provider, the
     * expected URI is content://com.google.contacts/#/entity (# is the ID value).
     */
    contactUri = Uri.withAppendedPath(
            contactUri,
            ContactsContract.Contacts.Entity.CONTENT_DIRECTORY
    )

    // Initializes the loader identified by LOADER_ID.
    loaderManager.initLoader(
            LOADER_ID,  // The identifier of the loader to initialize
            null,       // Arguments for the loader (in this case, none)
            this        // The context of the activity
    )

    // Creates a new cursor adapter to attach to the list view
    cursorAdapter = SimpleCursorAdapter(
            this,                       // the context of the activity
            R.layout.detail_list_item,  // the view item containing the detail widgets
            mCursor,                    // the backing cursor
            fromColumns,               // the columns in the cursor that provide the data
            toViews,                   // the views in the view item that display the data
            0)                          // flags

    // Sets the ListView's backing adapter.
    rawContactList.adapter = cursorAdapter
...
override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
    /*
     * Sets the columns to retrieve.
     * RAW_CONTACT_ID is included to identify the raw contact associated with the data row.
     * DATA1 contains the first column in the data row (usually the most important one).
     * MIMETYPE indicates the type of data in the data row.
     */
    val projection: Array<String> = arrayOf(
            ContactsContract.Contacts.Entity.RAW_CONTACT_ID,
            ContactsContract.Contacts.Entity.DATA1,
            ContactsContract.Contacts.Entity.MIMETYPE
    )

    /*
     * Sorts the retrieved cursor by raw contact id, to keep all data rows for a single raw
     * contact collated together.
     */
    val sortOrder = "${ContactsContract.Contacts.Entity.RAW_CONTACT_ID} ASC"

    /*
     * Returns a new CursorLoader. The arguments are similar to
     * ContentResolver.query(), except for the Context argument, which supplies the location of
     * the ContentResolver to use.
     */
    return CursorLoader(
            applicationContext, // The activity's context
            contactUri,        // The entity content URI for a single contact
            projection,         // The columns to retrieve
            null,               // Retrieve all the raw contacts and their data rows.
            null,               //
            sortOrder           // Sort by the raw contact ID.
    )
}
```

Quando o carregamento é finalizado, `LoaderManager` invoca um callback para `onLoadFinished()`. Um dos argumentos de entrada nesse método é um `Cursor` com os resultados da consulta. No seu aplicativo, você pode receber os dados desse `Cursor` para exibi-los ou trabalhar com eles posteriormente.

### Modificação em lote

Sempre que possível, deve-se inserir, atualizar e excluir dados no Provedor de contatos em “modo de lote”, criando uma `ArrayList` de objetos `ContentProviderOperation` e chamando `applyBatch()`. Como o Provedor de contatos realiza todas as operações em um `applyBatch()` em uma única transação, as modificações nunca deixarão o repositório de contatos em um estado inconsistente. As modificações em lote também facilitam a inserção de um contato bruto e seus dados de detalhe ao mesmo tempo.

**Observação:** para modificar um contato bruto *exclusivo*, considere enviar um intent ao aplicativo de contatos do dispositivo em vez de realizar a modificação no aplicativo. Isso é descrito em mais detalhes na seção [Recuperação e modificação com intents](https://developer.android.com/guide/topics/providers/contacts-provider#Intents).

#### Pontos de rendimento

As modificações em lote que contiverem muitas operações podem bloquear outros processos, resultando em uma experiência geral ruim para o usuário. Para organizar todas as modificações a realizar no menor número de listas separadas possível e, ao mesmo tempo, evitar o bloqueio do sistema, é preciso definir **pontos de rendimento** para uma ou mais operações. Pontos de rendimento são objetos `ContentProviderOperation` que têm seu valor `isYieldAllowed()` definido como `true`. Quando o Provedor de contatos encontra um ponto de rendimento, ele pausa o trabalho para permitir a execução de outros processos e encerra a transação em curso. Quando o provedor retorna, ele continua na próxima operação da `ArrayList` e inicia uma nova transação.

Os pontos de rendimento resultam em mais de uma transação por chamada de `applyBatch()`. Por isso, é preciso definir um ponto de rendimento para a última operação de um conjunto de linhas relacionadas. Por exemplo, é preciso definir um ponto de rendimento para a última operação em um conjunto que adicione linhas de um contato bruto e linhas de dados associados a ele, ou para a última operação de um conjunto de linhas relacionadas a um único contato.

Os pontos de rendimento também são uma unidade de operação atômica. Todos os acessos entre dois pontos de rendimento terão sucesso ou falharão como uma unidade. Se não houver pontos de rendimento definidos, a menor operação atômica será todo o lote de operações. Se forem usados pontos de rendimento, eles evitarão que as operações prejudiquem o desempenho do sistema e, ao mesmo tempo, garantirão que o subconjunto de operações seja atômico.

#### Referências de retorno da modificação

Ao inserir uma nova linha de contato bruto e as linhas de dados associados como um conjunto de objetos `ContentProviderOperation`, é preciso vincular as linhas de dados à linha de contato bruto pela inserção do valor `_ID` do contato bruto como o valor `RAW_CONTACT_ID`. Contudo, esse valor não está disponível ao criar a `ContentProviderOperation` para a linha de dados porque `ContentProviderOperation` ainda não foi aplicada à linha de contato bruto. Para trabalhar com isso, a classe `ContentProviderOperation.Builder` tem o método `withValueBackReference()`. Esse método permite a inserção ou modificação de uma coluna com o resultado de uma operação anterior.

O método `withValueBackReference()` tem dois argumentos:

- `key`

  É a chave de um par de chave-valor. O valor desse argumento deve ser o nome de uma coluna na tabela que será modificada.

- `previousResult`

  É o índice com base 0 de um valor em uma matriz de objetos `ContentProviderResult` de `applyBatch()`. Quando as operações em lote são aplicadas, o resultado de cada operação é armazenado em uma matriz intermediária de resultados. O valor `previousResult` é o índice de um desses resultados, que é recuperado e armazenado com o valor `key`. Isso permite a inserção de um novo registro de contato bruto e a recuperação do valor `_ID`, seguido de uma “referência de retorno” ao valor ao adicionar uma linha `ContactsContract.Data`.Toda a matriz de resultados é criada ao chamar `applyBatch()` pela primeira vez, com tamanho igual ao da `ArrayList` de objetos `ContentProviderOperation` fornecidos. No entanto, todos os elementos na matriz de resultados são definidos como `null` e, se houver uma tentativa de referência de retorno a um resultado de uma operação ainda não realizada, `withValueBackReference()` gera uma `Exception`.

Os snippets a seguir mostram como inserir um novo contato bruto e dados em lote. Eles contêm o código que estabelece um ponto de rendimento e usam uma referência de retorno.

O primeiro snippet recupera dados de contato da IU. Nesse momento, o usuário já selecionou a conta a que o novo contato bruto deve ser adicionado.

[Kotlin](https://developer.android.com/guide/topics/providers/contacts-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/contacts-provider#java)

```kotlin
// Creates a contact entry from the current UI values, using the currently-selected account.
private fun createContactEntry() {
    /*
     * Gets values from the UI
     */
    val name = contactNameEditText.text.toString()
    val phone = contactPhoneEditText.text.toString()
    val email = contactEmailEditText.text.toString()

    val phoneType: String = contactPhoneTypes[mContactPhoneTypeSpinner.selectedItemPosition]

    val emailType: String = contactEmailTypes[mContactEmailTypeSpinner.selectedItemPosition]
```

O próximo snippet cria uma operação para inserir o contato bruto na tabela `ContactsContract.RawContacts`:

[Kotlin](https://developer.android.com/guide/topics/providers/contacts-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/contacts-provider#java)

```kotlin
    /*
     * Prepares the batch operation for inserting a new raw contact and its data. Even if
     * the Contacts Provider does not have any data for this person, you can't add a Contact,
     * only a raw contact. The Contacts Provider will then add a Contact automatically.
     */

    // Creates a new array of ContentProviderOperation objects.
    val ops = arrayListOf<ContentProviderOperation>()

    /*
     * Creates a new raw contact with its account type (server type) and account name
     * (user's account). Remember that the display name is not stored in this row, but in a
     * StructuredName data row. No other data is required.
     */
    var op: ContentProviderOperation.Builder =
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, selectedAccount.name)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, selectedAccount.type)

    // Builds the operation and adds it to the array of operations
    ops.add(op.build())
```

Em seguida, o código cria linhas de dados para as linhas de nome de exibição, telefone e e-mail.

Cada objeto construtor de operações usa `withValueBackReference()` para receber o `RAW_CONTACT_ID`. Os pontos de referência voltam ao objeto `ContentProviderResult` da primeira operação, que adiciona a linha de contato bruto e retorna seu novo valor `_ID`. Como resultado, cada linha de dados é automaticamente vinculada por meio do `RAW_CONTACT_ID` à nova linha `ContactsContract.RawContacts` a que ela pertence.

O objeto `ContentProviderOperation.Builder` que adiciona a linha de e-mail é sinalizado com `withYieldAllowed()`, que define um ponto de rendimento:

[Kotlin](https://developer.android.com/guide/topics/providers/contacts-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/contacts-provider#java)

```kotlin
    // Creates the display name for the new raw contact, as a StructuredName data row.
    op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            /*
             * withValueBackReference sets the value of the first argument to the value of
             * the ContentProviderResult indexed by the second argument. In this particular
             * call, the raw contact ID column of the StructuredName data row is set to the
             * value of the result returned by the first operation, which is the one that
             * actually adds the raw contact row.
             */
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

            // Sets the data row's MIME type to StructuredName
            .withValue(ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)

            // Sets the data row's display name to the name in the UI.
            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)

    // Builds the operation and adds it to the array of operations
    ops.add(op.build())

    // Inserts the specified phone number and type as a Phone data row
    op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            /*
             * Sets the value of the raw contact id column to the new raw contact ID returned
             * by the first operation in the batch.
             */
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

            // Sets the data row's MIME type to Phone
            .withValue(ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)

            // Sets the phone number and type
            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType)

    // Builds the operation and adds it to the array of operations
    ops.add(op.build())

    // Inserts the specified email and type as a Phone data row
    op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            /*
             * Sets the value of the raw contact id column to the new raw contact ID returned
             * by the first operation in the batch.
             */
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

            // Sets the data row's MIME type to Email
            .withValue(ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)

            // Sets the email address and type
            .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
            .withValue(ContactsContract.CommonDataKinds.Email.TYPE, emailType)

    /*
     * Demonstrates a yield point. At the end of this insert, the batch operation's thread
     * will yield priority to other threads. Use after every set of operations that affect a
     * single contact, to avoid degrading performance.
     */
    op.withYieldAllowed(true)

    // Builds the operation and adds it to the array of operations
    ops.add(op.build())
```

O último snippet mostra a chamada para `applyBatch()` que insere as novas linhas de contato bruto e de dados.

[Kotlin](https://developer.android.com/guide/topics/providers/contacts-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/contacts-provider#java)

```kotlin
    // Ask the Contacts Provider to create a new contact
    Log.d(TAG, "Selected account: ${mSelectedAccount.name} (${mSelectedAccount.type})")
    Log.d(TAG, "Creating contact: $name")

    /*
     * Applies the array of ContentProviderOperation objects in batch. The results are
     * discarded.
     */
    try {
        contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
    } catch (e: Exception) {
        // Display a warning
        val txt: String = getString(R.string.contactCreationFailure)
        Toast.makeText(applicationContext, txt, Toast.LENGTH_SHORT).show()

        // Log exception
        Log.e(TAG, "Exception encountered while inserting contact: $e")
    }
}
```

As operações de lote também permitem a implementação de **controle otimista de simultaneidade**, um método de aplicar transações de modificação sem precisar bloquear o repositório subjacente. Para usar esse método, aplique a transação e, em seguida, verifique se há outras modificações que possam ter sido realizadas ao mesmo tempo. Se ficar determinado que houve uma modificação incoerente, reverte-se a transação e tenta-se novamente.

O controle otimista de simultaneidade é útil para dispositivos móveis em que haja somente um usuário por vez e que sejam raros os acessos simultâneos a um repositório de dados. Como os bloqueios não são usados, não há tempo gasto em bloqueios de configuração nem espera para que outras transações liberem os respectivos bloqueios.

Para usar o controle otimista de simultaneidade ao atualizar uma única linha `ContactsContract.RawContacts`, siga estas etapas:

1. Recupere a coluna `VERSION` do contato bruto em conjunto com os outros dados recuperados.
2. Crie um objeto `ContentProviderOperation.Builder` adequado para forçar uma restrição com o método `newAssertQuery(Uri)`. Para o URI de conteúdo, use `RawContacts.CONTENT_URI` com o `_ID` do contato bruto anexado a ele.
3. Para o objeto `ContentProviderOperation.Builder`, chame `withValue()` para comparar a coluna `VERSION` com o número da versão que acabou de recuperar.
4. Para o mesmo `ContentProviderOperation.Builder`, chame `withExpectedCount()` para garantir que somente uma linha seja testada por essa instrução.
5. Chame `build()` para criar o objeto `ContentProviderOperation`. Em seguida, adicione esse objeto como o primeiro objeto na `ArrayList` passada para `applyBatch()`.
6. Aplique a transação em lote.

Se a linha de contato bruto for atualizada por outra operação entre o momento da leitura da linha e o momento de sua modificação, a `ContentProviderOperation` de “assert” falhará e todo o lote de operações será cancelado. Depois disso, as opções são tentar novamente ou tomar outra medida.

O snippet a seguir demonstra como criar uma `ContentProviderOperation` de “assert” após consultar um contato bruto exclusivo usando um `CursorLoader`:

[Kotlin](https://developer.android.com/guide/topics/providers/contacts-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/contacts-provider#java)

```kotlin
/*
 * The application uses CursorLoader to query the raw contacts table. The system calls this method
 * when the load is finished.
 */
override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor) {
    // Gets the raw contact's _ID and VERSION values
    rawContactID = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID))
    mVersion = cursor.getInt(cursor.getColumnIndex(SyncColumns.VERSION))
}

...

// Sets up a Uri for the assert operation
val rawContactUri: Uri = ContentUris.withAppendedId(
        ContactsContract.RawContacts.CONTENT_URI,
        rawContactID
)

// Creates a builder for the assert operation
val assertOp: ContentProviderOperation.Builder =
        ContentProviderOperation.newAssertQuery(rawContactUri).apply {
            // Adds the assertions to the assert operation: checks the version
            withValue(SyncColumns.VERSION, mVersion)

            // and count of rows tested
            withExpectedCount(1)
        }

// Creates an ArrayList to hold the ContentProviderOperation objects
val ops = arrayListOf<ContentProviderOperation>()

ops.add(assertOp.build())

// You would add the rest of your batch operations to "ops" here

...

// Applies the batch. If the assert fails, an Exception is thrown
try {
    val results: Array<ContentProviderResult> = contentResolver.applyBatch(AUTHORITY, ops)
} catch (e: OperationApplicationException) {
    // Actions you want to take if the assert operation fails go here
}
```

### Recuperação e modificação com intents

Enviar um intent ao aplicativo de contatos do dispositivo permite o acesso indireto ao Provedor de contatos. O intent inicia a IU do aplicativo de contatos do dispositivo, em que os usuários podem fazer tarefas relacionadas a contatos. Com esse tipo de acesso, os usuários podem:

- Selecionar um contato de uma lista e retorná-lo ao aplicativo para trabalhos futuros.
- Editar dados de um contato existente.
- Inserir um novo contato bruto para quaisquer das suas contas.
- Excluir um contato ou dados dos contatos.

Se o usuário estiver inserindo ou atualizando dados, será possível coletar os dados antes e enviá-los como parte do intent.

Ao usar intents para acessar o Provedor de contatos por meio do aplicativo de contatos do dispositivo, não é necessário criar a própria IU ou código para acessar o provedor. Também não é necessário solicitar permissão de leitura e gravação ao provedor. O aplicativo de contatos do dispositivo pode delegar permissões de leitura de um contato e, pelo fato de você fazer modificações no provedor por meio de outro aplicativo, não é necessário ter permissões de gravação.

O processo geral de envio de um intent para acessar um provedor é descrito detalhadamente no guia [Fundamentos do provedor de conteúdo](https://developer.android.com/guide/topics/providers/content-provider-basics), seção “Acesso a dados via intents”. Os valores de ação, do tipo MIME e dos dados usados para as tarefas disponíveis são resumidos na tabela 4, e os valores extras que podem ser usados com `putExtra()` são listados na documentação de referência de `ContactsContract.Intents.Insert`:

**Tabela 4.** Intents do Provedor de contatos.

| Tarefa                                                | Ação                    | Dados                                                        | Tipo MIME                                                    | Observações                                                  |
| :---------------------------------------------------- | :---------------------- | :----------------------------------------------------------- | :----------------------------------------------------------- | :----------------------------------------------------------- |
| **Escolha um contato de uma lista**                   | `ACTION_PICK`           | Pode ser:`Contacts.CONTENT_URI`, que exibe uma lista de contatos.`Phone.CONTENT_URI`, que exibe uma lista de números de telefone de um contato bruto.`StructuredPostal.CONTENT_URI`, que exibe uma lista de endereços de correspondência de um contato bruto.`Email.CONTENT_URI`, que exibe uma lista de endereços de e-mail de um contato bruto. | Não usado                                                    | Exibe uma lista de contatos brutos ou uma lista de dados de um contato bruto conforme o tipo do URI de conteúdo fornecido.Chame `startActivityForResult()`, que retorna o URI de conteúdo da linha selecionada. A forma do URI é o URI de conteúdo da tabela com o `LOOKUP_ID` da linha anexado a ele. O aplicativo de contatos do dispositivo delega permissões de leitura e gravação a esse URI de conteúdo pelo tempo que a atividade durar. Consulte o guia [Fundamentos do provedor de conteúdo](https://developer.android.com/guide/topics/providers/content-provider-basics) para ver mais detalhes. |
| **Inserir um novo contato bruto**                     | `Insert.ACTION`         | N/A                                                          | `RawContacts.CONTENT_TYPE`, tipo MIME para um grupo de contatos brutos. | Exibe a tela **Add Contact** do aplicativo de contatos do dispositivo. São exibidos os valores extras adicionados ao intent. Se enviado com `startActivityForResult()`, o URI de conteúdo do contato bruto recentemente adicionado é passado de volta ao método de callback `onActivityResult()` no argumento `Intent`, no campo “data”. Para receber o valor, chame `getData()`. |
| **Edição de um contato**                              | `ACTION_EDIT`           | `CONTENT_LOOKUP_URI` do contato. A atividade do editor permitirá que o usuário edite os dados associados a esse contato. | `Contacts.CONTENT_ITEM_TYPE`, um único contato.              | Exibe a tela “Edit Contact” no aplicativo de contatos. São exibidos os valores extras adicionados ao intent. Quando o usuário clica em **Done** para salvar as edições, a atividade retorna ao primeiro plano. |
| **Exibe um seletor que também pode adicionar dados.** | `ACTION_INSERT_OR_EDIT` | N/A                                                          | `CONTENT_ITEM_TYPE`                                          | Esse intent sempre exibe a tela do seletor do aplicativo de contatos. O usuário pode selecionar um contato para editar ou adicionar um novo. É exibida a tela de edição ou de adição, conforme a escolha do usuário, e são exibidos os dados extras passados para ao intent. Se o aplicativo exibir dados de contato como um e-mail ou número de telefone, use esse intent para permitir que o usuário adicione os dados a um contato existente.**Observação:** não é necessário enviar nenhum valor de nome dos extras do intent, porque o usuário sempre seleciona um nome existente ou adiciona um novo. Além disso, se você enviar um nome e o usuário escolher editar, o aplicativo de contatos exibirá o nome enviado, substituindo o valor anterior. Se o usuário não notar isso e salvar a edição, o valor antigo será perdido. |

O aplicativo de contatos do dispositivo não permite a exclusão de um contato bruto ou de seus dados com um intent. Em vez disso, para excluir um contato bruto, use `ContentResolver.delete()` ou `ContentProviderOperation.newDelete()`.

O snippet a seguir mostra como construir e enviar um intent que insere um novo contato bruto e dados:

[Kotlin](https://developer.android.com/guide/topics/providers/contacts-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/contacts-provider#java)

```kotlin
// Gets values from the UI
val name = contactNameEditText.text.toString()
val phone = contactPhoneEditText.text.toString()
val email = contactEmailEditText.text.toString()

val company = companyName.text.toString()
val jobtitle = jobTitle.text.toString()

/*
 * Demonstrates adding data rows as an array list associated with the DATA key
 */

// Defines an array list to contain the ContentValues objects for each row
val contactData = arrayListOf<ContentValues>()

/*
 * Defines the raw contact row
 */

// Sets up the row as a ContentValues object
val rawContactRow = ContentValues().apply {
    // Adds the account type and name to the row
    put(ContactsContract.RawContacts.ACCOUNT_TYPE, selectedAccount.type)
    put(ContactsContract.RawContacts.ACCOUNT_NAME, selectedAccount.name)
}

// Adds the row to the array
contactData.add(rawContactRow)

/*
 * Sets up the phone number data row
 */

// Sets up the row as a ContentValues object
val phoneRow = ContentValues().apply {
    // Specifies the MIME type for this data row (all data rows must be marked by their type)
    put(ContactsContract.Data.MIMETYPE,ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)

    // Adds the phone number and its type to the row
    put(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
}

// Adds the row to the array
contactData.add(phoneRow)

/*
 * Sets up the email data row
 */

// Sets up the row as a ContentValues object
val emailRow = ContentValues().apply {
    // Specifies the MIME type for this data row (all data rows must be marked by their type)
    put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)

    // Adds the email address and its type to the row
    put(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
}

// Adds the row to the array
contactData.add(emailRow)

// Creates a new intent for sending to the device's contacts application
val insertIntent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
    // Sets the MIME type to the one expected by the insertion activity
    type = ContactsContract.RawContacts.CONTENT_TYPE

    // Sets the new contact name
    putExtra(ContactsContract.Intents.Insert.NAME, name)

    // Sets the new company and job title
    putExtra(ContactsContract.Intents.Insert.COMPANY, company)
    putExtra(ContactsContract.Intents.Insert.JOB_TITLE, jobtitle)

    /*
    * Adds the array to the intent's extras. It must be a parcelable object in order to
    * travel between processes. The device's contacts app expects its key to be
    * Intents.Insert.DATA
    */
    putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, contactData)
}

// Send out the intent to start the device's contacts app in its add contact activity.
startActivity(insertIntent)
```

### Integridade dos dados

Como o repositório de contatos contém dados importantes e confidenciais que usuários esperam que estejam corretos e atualizados, o Provedor de contatos tem regras bem definidas para a integridade dos dados. É de sua responsabilidade obedecer a essas regras ao modificar dados de contatos. As regras importantes são listadas a seguir:

- Sempre adicione uma linha `ContactsContract.CommonDataKinds.StructuredName` para cada linha `ContactsContract.RawContacts` adicionada.

  Uma linha `ContactsContract.RawContacts` sem uma linha `ContactsContract.CommonDataKinds.StructuredName` na tabela `ContactsContract.Data` pode causar problemas durante a agregação.

- Sempre conecte novas linhas `ContactsContract.Data` às respectivas linhas pai `ContactsContract.RawContacts`.

  Uma linha `ContactsContract.Data` que não esteja conectada a um `ContactsContract.RawContacts` não será visível no aplicativo de contatos do dispositivo, o que poderá causar problemas com adaptadores de sincronização.

- Altere dados somente para seus contatos brutos.

  Lembre-se de que o Provedor de contatos normalmente gerencia dados de diversos tipos de conta e serviços on-line diferentes. É preciso garantir que o aplicativo modifique ou exclua somente dados de linhas de sua propriedade e que ele insira dados apenas com um tipo e nome de conta que você controla.

- Sempre use as constantes definidas em `ContactsContract` e suas subclasses de autoridades, URIs de conteúdo, caminhos de URI, nomes de coluna, tipos MIME e valores `TYPE`.

  O uso dessas constantes ajuda a evitar erros. Você também será notificado com avisos do compilador se uma das constantes estiver obsoleta.

### Linhas de dados personalizados

Ao criar e usar seus tipos MIME personalizados, você pode inserir, editar, excluir e recuperar suas linhas de dados na tabela `ContactsContract.Data`. As linhas são limitadas a usar a coluna definida em `ContactsContract.DataColumns`, embora seja possível mapear os nomes de coluna de tipo específico para os nomes de coluna padrão. No aplicativo de contatos do dispositivo, os dados das linhas são exibidos, mas não podem ser editados nem excluídos, e os usuários não podem inserir dados adicionais. Para permitir que usuários modifiquem suas linhas de dados personalizados, é necessário fornecer uma atividade do editor no próprio aplicativo.

Para exibir os dados personalizados, forneça um arquivo `contacts.xml` contendo um elemento `<ContactsAccountType>` ou um ou mais dos elementos filho `<ContactsDataKind>`. Isso é abordado com mais detalhes na seção [` element`](https://developer.android.com/guide/topics/providers/contacts-provider#SocialStreamDataKind).

Para saber mais sobre tipos MIME personalizados, leia o guia [Criação de um provedor de conteúdo](https://developer.android.com/guide/topics/providers/content-provider-creating).

## Adaptadores de sincronização do Provedor de contatos

O Provedor de contatos foi projetado especificamente para processar a **sincronização** de dados de contatos entre um dispositivo e um serviço on-line. Isso permite que usuários baixem dados existentes em um novo dispositivo e transfiram dados existentes a uma nova conta. A sincronização também garante que usuários tenham os dados mais recentes à mão, independentemente da origem de adições e alterações. Outra vantagem da sincronização é a disponibilidade dos dados dos contatos mesmo quando o dispositivo não está conectado à rede.

Embora seja possível implementar a sincronização de diversos modos, o sistema Android fornece uma biblioteca de sincronização de plug-in que automatiza as seguintes tarefas:

- Verificação da disponibilidade da rede.
- Agendamento e execução de sincronizações, com base nas preferências do usuário.
- Reinicialização de sincronizações que foram interrompidas.

Para usar essa biblioteca, deve-se fornecer um plug-in do adaptador de sincronização. Cada adaptador de sincronização é exclusivo de um serviço e um provedor de conteúdo, mas pode processar diversos nomes de conta do mesmo serviço. A biblioteca também permite diversos adaptadores de sincronização para o mesmo serviço e provedor.

### Classes e arquivos do adaptador de sincronização

O adaptador de sincronização é implementado como uma subclasse de `AbstractThreadedSyncAdapter` e instalado como parte do aplicativo Android. O sistema coleta dados do adaptador de sincronização dos elementos no manifesto do aplicativo e de um arquivo XML especial direcionado pelo manifesto. O arquivo XML define o tipo de conta do serviço on-line e a autoridade do provedor de conteúdo que, juntos, identificam exclusivamente o adaptador. O adaptador de sincronização não fica ativo até que o usuário adicione uma conta para o tipo de conta do adaptador de sincronização e habilite a sincronização para o provedor de conteúdo com que o adaptador se sincroniza. Nesse ponto, o sistema começa a gerenciar o adaptador, chamando-o quando necessário para estabelecer a sincronização entre o provedor de conteúdo e o servidor.

**Observação:** usar um tipo de conta como parte da identificação do adaptador de sincronização permite que o sistema detecte e agrupe adaptadores de sincronização que acessam diferentes serviços da mesma organização. Por exemplo, todos os adaptadores de sincronização dos serviços on-line do Google têm o mesmo tipo de conta `com.google`. Quando o usuário adiciona uma Conta do Google aos dispositivos, todos os adaptadores de sincronização dos serviços Google instalados são listados juntos. Cada um dos listados sincroniza com um provedor de conteúdo diferente no dispositivo.

Como a maioria dos serviços exige que usuários verifiquem a identidade antes de acessar os dados, o sistema Android oferece uma biblioteca de autenticação similar à biblioteca dos adaptadores de sincronização e muitas vezes usada junto com eles. A biblioteca de autenticação usa autenticadores de plug-ins que são subclasses de `AbstractAccountAuthenticator`. Um autenticador verifica a identidade do usuário nas seguintes etapas:

1. Coleta nome, senha ou informação similar do usuário (as **credenciais** do usuário).
2. Envia as credenciais para o serviço.
3. Avalia a resposta do serviço.

Se o serviço aceitar as credenciais, o autenticador poderá armazená-las para uso futuro. Devido à biblioteca do autenticador de plug-ins, `AccountManager` pode conferir acesso a quaisquer tokens de autenticação compatíveis com um autenticador que escolha expô-los, como os tokens de autenticação OAuth2.

Embora as autenticações não sejam necessárias, a maioria dos serviços de contato as usam. Contudo, não é obrigatório usar a estrutura de autenticação do Android para efetuá-las.

### Implementação do adaptador de sincronização

Para implementar um adaptador de sincronização para o Provedor de contatos, primeiramente é necessário criar um aplicativo Android que contenha o seguinte:

- Um componente `Service` que responde a solicitações do sistema para vincular ao adaptador de sincronização.

  Quando o sistema quer executar uma sincronização, ele chama o método `onBind()` do serviço para receber um `IBinder` para o adaptador de sincronização. Isso permite que o sistema efetue chamadas entre processos aos métodos do adaptador.

- O adaptador de sincronização atual, implementado como uma subclasse concreta de `AbstractThreadedSyncAdapter`.

  Essa classe realiza o trabalho de baixar dados do servidor, fazer upload de dados do dispositivo e resolver conflitos. A principal tarefa do adaptador é feita no método `onPerformSync()`. Essa classe deve ser instanciada como um singleton.

- Uma subclasse de `Application`.

  Essa classe atua como uma fábrica para o singleton do adaptador de sincronização. Use o método `onCreate()` para instanciar o adaptador de sincronização e fornecer um método “coletor” estático para retornar o singleton ao método `onBind()` do serviço do adaptador de sincronização.

- **Opcional:** um componente `Service` que responde a solicitações do sistema para autenticação do usuário.

  O `AccountManager` ativa esse serviço para iniciar o processo de autenticação. O método `onCreate()` do serviço instancia um objeto autenticador. Quando o sistema quer autenticar uma conta de usuário para o adaptador de sincronização do aplicativo, ele chama o método `onBind()` do serviço para conseguir um `IBinder` para o autenticador. Isso permite que o sistema efetue chamadas entre processos aos métodos do autenticador.

- **Opcional:** uma subclasse concreta de `AbstractAccountAuthenticator` que processa solicitações de autenticação.

  Essa classe fornece métodos que chamam `AccountManager` para autenticar as credenciais do usuário no servidor. Os detalhes desse processo de autenticação variam amplamente, com base na tecnologia em uso no servidor. Consulte a documentação do seu software servidor para ver mais informações sobre autenticação.

- Os arquivos XML que definem o adaptador de sincronização e o autenticador para o sistema.

  Os componentes de serviço do adaptador de sincronização e do autenticador descritos anteriormente são definidos em elementos `<service>` no manifesto do aplicativo. Esses elementos contêm elementos filho `<meta-data>` que fornecem dados específicos ao sistema:O elemento `<meta-data>` de serviço do adaptador de sincronização aponta para `res/xml/syncadapter.xml` do arquivo XML. Em troca, esse arquivo especifica um URI para o serviço Web que será sincronizado com o Provedor de contatos e um tipo de conta.**Opcional:** o elemento `<meta-data>` do autenticador aponta para o arquivo XML `res/xml/authenticator.xml`. Em troca, esse arquivo especifica o tipo de conta compatível com esse autenticador, bem como recursos de IU que aparecem durante o processo de autenticação. O tipo de conta especificado nesse elemento deve ser o mesmo que o especificado para o adaptador de sincronização.

## Dados de streams sociais

As tabelas android.provider.ContactsContract.StreamItems e android.provider.ContactsContract.StreamItemPhotos gerenciam dados recebidos de redes sociais. É possível criar um adaptador de sincronização que adicione dados de stream da sua rede a essas tabelas ou ler dados de stream dessas tabelas e exibi-los no seu aplicativo, ou ambas as opções. Com esses recursos, os serviços e aplicativos de redes sociais podem ser integrados na experiência das redes sociais no Android.

### Textos de streams sociais

Itens de stream sempre são associados a um contato bruto. O android.provider.ContactsContract.StreamItemsColumns#RAW_CONTACT_ID se conecta ao valor `_ID` do contato bruto. O tipo e o nome da conta do contato bruto também são armazenados na linha do item de stream.

Armazene os dados do seu stream nas colunas a seguir:

- android.provider.ContactsContract.StreamItemsColumns#ACCOUNT_TYPE

  **Obrigatórias.** É o tipo de conta do usuário do contato bruto associado a esse item de stream. Lembre-se de definir esse valor ao inserir um item de stream.

- android.provider.ContactsContract.StreamItemsColumns#ACCOUNT_NAME

  **Obrigatórias.** É o nome de conta do usuário do contato bruto associado a esse item de stream. Lembre-se de definir esse valor ao inserir um item de stream.

- Colunas identificadoras

  **Obrigatórias.** É necessário inserir as colunas identificadoras a seguir ao inserir um item de stream.android.provider.ContactsContract.StreamItemsColumns#CONTACT_ID: é o valor android.provider.BaseColumns#_ID do contato a que esse item de stream está associado.android.provider.ContactsContract.StreamItemsColumns#CONTACT_LOOKUP_KEY: é o valor android.provider.ContactsContract.ContactsColumns#LOOKUP_KEY do contato a que esse item de stream está associado.android.provider.ContactsContract.StreamItemsColumns#RAW_CONTACT_ID: é o valor android.provider.BaseColumns#_ID do contato bruto a que esse item de stream está associado.

- android.provider.ContactsContract.StreamItemsColumns#COMMENTS

  Opcional. Armazena informações resumidas que podem ser exibidas no início do item de stream.

- android.provider.ContactsContract.StreamItemsColumns#TEXT

  É o texto do item de stream, o conteúdo que foi publicado pela fonte do item ou uma descrição de alguma ação que gerou o item de stream. Essa coluna pode conter imagens de recurso incorporadas e de qualquer formato que possam ser renderizadas `fromHtml()`. O provedor pode truncar ou abreviar conteúdos longos, mas ele tentará evitar quebrar as tags.

- android.provider.ContactsContract.StreamItemsColumns#TIMESTAMP

  É uma string de texto contendo o tempo em que o item de stream foi inserido ou atualizado, em forma de *milissegundos*, desde a época. Aplicativos que inserem ou atualizam itens de stream são responsáveis por manter essa coluna. Ela não é mantida automaticamente pelo Provedor de contatos.

Para exibir informações de identificação dos itens de stream, use android.provider.ContactsContract.StreamItemsColumns#RES_ICON, android.provider.ContactsContract.StreamItemsColumns#RES_LABEL e android.provider.ContactsContract.StreamItemsColumns#RES_PACKAGE para vincular a recursos do aplicativo.

A tabela android.provider.ContactsContract.StreamItems também contém as colunas de android.provider.ContactsContract.StreamItemsColumns#SYNC1 a android.provider.ContactsContract.StreamItemsColumns#SYNC4 para uso exclusivo dos adaptadores de sincronização.

### Fotos de streams sociais

A tabela android.provider.ContactsContract.StreamItemPhotos armazena fotos associadas a um item de stream. A coluna android.provider.ContactsContract.StreamItemPhotosColumns#STREAM_ITEM_ID da tabela se vincula a valores da coluna `_ID` da tabela android.provider.ContactsContract.StreamItems. Referências de fotografias são armazenadas na tabela nestas colunas:

- Coluna android.provider.ContactsContract.StreamItemPhotos#PHOTO (um BLOB).

  Representação binária da foto, redimensionada pelo provedor para armazenar e exibir. Essa coluna está disponível para compatibilidade retroativa com versões anteriores do Provedor de contatos que a usavam para armazenar fotos. Contudo, na versão atual, não se deve usar essa coluna para armazenar fotos. Em vez disso, use android.provider.ContactsContract.StreamItemPhotosColumns#PHOTO_FILE_ID ou android.provider.ContactsContract.StreamItemPhotosColumns#PHOTO_URI (ambos são descritos nos pontos a seguir) para armazenar fotos em um arquivo. Essa coluna passa a conter uma miniatura da foto, que estará disponível para leitura.

- android.provider.ContactsContract.StreamItemPhotosColumns#PHOTO_FILE_ID

  É um identificador numérico de uma foto de um contato bruto. Anexe esse valor à constante `DisplayPhoto.CONTENT_URI` para receber um URI de conteúdo direcionado para um único arquivo de foto e, em seguida, chame `openAssetFileDescriptor()` para receber um identificador para o arquivo de foto.

- android.provider.ContactsContract.StreamItemPhotosColumns#PHOTO_URI

  É um URI de conteúdo direcionado diretamente para o arquivo de foto da foto representada por essa linha. Chame `openAssetFileDescriptor()` com esse URI para conseguir um identificador para o arquivo de foto.

### Uso de tabelas de streams sociais

Essas tabelas funcionam como as outras tabelas principais do Provedor de Contatos, exceto que:

- exigem permissões de acesso adicionais. Para ler o conteúdo delas, o aplicativo deve ter a permissão android.Manifest.permission#READ_SOCIAL_STREAM. Para modificá-las, o aplicativo precisa ter a permissão android.Manifest.permission#WRITE_SOCIAL_STREAM;
- para a tabela android.provider.ContactsContract.StreamItems, o número de linhas armazenadas para cada contato bruto é limitado. Ao atingir o limite, o Provedor de contatos abre espaço para novas linhas de item de stream excluindo automaticamente as linhas que têm o android.provider.ContactsContract.StreamItemsColumns#TIMESTAMP mais antigo. Para conhecer o limite, faça uma consulta ao URI de conteúdo android.provider.ContactsContract.StreamItems#CONTENT_LIMIT_URI. É possível deixar todos os argumentos, exceto o URI de conteúdo, definidos como `null`. A consulta retorna um Cursor contendo uma linha única com a coluna única android.provider.ContactsContract.StreamItems#MAX_ITEMS.

A classe android.provider.ContactsContract.StreamItems.StreamItemPhotos define uma subtabela de android.provider.ContactsContract.StreamItemPhotos que contém as linhas de foto de um único item de stream.

### Interações de streams sociais

Os dados de streams sociais gerenciados pelo Provedor de contatos, em conjunto com o aplicativo de contatos do dispositivo, oferecem um método poderoso de conexão do sistema de redes sociais com contatos existentes. Os seguintes recursos estão disponíveis:

- Ao sincronizar seu serviço de rede social ao Provedor de contatos com um adaptador de sincronização, é possível recuperar atividades recentes dos contatos de um usuário e armazená-las nas tabelas android.provider.ContactsContract.StreamItems e android.provider.ContactsContract.StreamItemPhotos para uso posterior.
- Além da sincronização regular, é possível ativar o adaptador de sincronização para recuperar dados adicionais quando o usuário seleciona um contato para exibir. Isso permite que o adaptador de sincronização recupere fotos de alta resolução e os itens de stream mais recentes do contato.
- Ao registrar uma notificação com o aplicativo de contatos do dispositivo e o Provedor de contatos, é possível *recuperar* um intent quando um contato é exibido e, nesse ponto, atualizar o status do contato pelo serviço. Essa abordagem pode ser mais rápida e usar menos largura de banda do que realizar uma sincronização completa com um adaptador de sincronização.
- Os usuários podem adicionar um contato ao seu serviço de rede social enquanto o exibem no aplicativo de contatos do dispositivo. É possível ativar isso com o recurso “convidar contato”, que é ativado com uma combinação de uma atividade que adiciona um contato existente à sua rede, um arquivo XML que fornece o aplicativo de contatos do dispositivo e o Provedor de contatos com os detalhes do aplicativo.

A sincronização regular de itens de stream com o Provedor de contatos é igual a outras sincronizações. Para saber mais sobre sincronizações, consulte a seção [Adaptadores de sincronização do Provedor de contatos](https://developer.android.com/guide/topics/providers/contacts-provider#SyncAdapters). O registro de notificações e o convite a contatos são abordados nas próximas duas seções.

#### Registro para processar exibições de redes sociais

Para registrar o adaptador de sincronização para receber notificações quando o usuário exibe um contato que é gerenciado pelo adaptador de sincronização:

1. Crie um arquivo chamado `contacts.xml` no diretório `res/xml/` do projeto. Se você já tiver esse arquivo, pule esta etapa.
2. Nesse arquivo, adicione o elemento `<ContactsAccountType xmlns:android="http://schemas.android.com/apk/res/android">`. Se esse elemento já existir, pule esta etapa.
3. Para registrar um serviço que seja notificado quando o usuário abre uma página de detalhes do contato no aplicativo de contatos do dispositivo, adicione o atributo `viewContactNotifyService="*serviceclass*"` ao elemento, em que `*serviceclass*` é o nome de classe totalmente qualificado do serviço que deve receber o intent do aplicativo de contatos do dispositivo. Para o serviço de notificação, use a classe que estende `IntentService` para permitir que o serviço receba intents. Os dados nos intents recebidos contêm o URI de conteúdo do contato bruto em que o usuário clicou. No serviço de notificação, é possível vincular e chamar o adaptador de sincronização para atualizar os dados do contato bruto.

Para registrar uma atividade a ser chamada quando o usuário clica em um item de stream, em uma foto ou em ambos:

1. Crie um arquivo chamado `contacts.xml` no diretório `res/xml/` do projeto. Se você já tiver esse arquivo, pule esta etapa.
2. Nesse arquivo, adicione o elemento `<ContactsAccountType xmlns:android="http://schemas.android.com/apk/res/android">`. Se esse elemento já existir, pule esta etapa.
3. Para registrar uma das atividades para processar o usuário que clica em um item de stream no aplicativo de contatos do dispositivo, adicione o atributo `viewStreamItemActivity="*activityclass*"` ao elemento, em que `*activityclass*` é o nome de classe totalmente qualificado da atividade que deve receber o intent do aplicativo de contatos do dispositivo.
4. Para registrar uma das atividades para processar o usuário que clica em uma foto de stream no aplicativo de contatos do dispositivo, adicione o atributo `viewStreamItemPhotoActivity="*activityclass*"` ao elemento, em que `*activityclass*` é o nome de classe totalmente qualificado da atividade que deve receber o intent do aplicativo de contatos do dispositivo.

O elemento `<ContactsAccountType>` é descrito em mais detalhes na seção [Elemento ](https://developer.android.com/guide/topics/providers/contacts-provider#SocialStreamAcctType).

O intent recebido contém o URI de conteúdo do item ou foto em que o usuário clicou. Para ter atividades separadas para itens de texto e para fotos, use os dois atributos no mesmo arquivo.

#### Interação com o serviço de redes sociais

Os usuários não precisam sair do aplicativo de contatos do dispositivo para convidar um contato para seu site de rede social. Em vez disso, o aplicativo de contatos do dispositivo pode enviar um intent para convidar o contato para uma das atividades. Para configurar isso:

1. Crie um arquivo chamado `contacts.xml` no diretório `res/xml/` do projeto. Se você já tiver esse arquivo, pule esta etapa.

2. Nesse arquivo, adicione o elemento `<ContactsAccountType xmlns:android="http://schemas.android.com/apk/res/android">`. Se esse elemento já existir, pule esta etapa.

3. Adicione os seguintes atributos:

   - `inviteContactActivity="*activityclass*"`
   - `inviteContactActionLabel="@string/*invite_action_label*"`

   O valor

    

   ```
   activityclass
   ```

    

   é o nome de classe totalmente qualificado da atividade que deve receber o intent. O valor

    

   ```
   invite_action_label
   ```

    

   é uma string de texto exibida no menu

    

   Add Connection

    

   no aplicativo de contatos do dispositivo.

**Observação:** `ContactsSource` é um nome de tag obsoleto para `ContactsAccountType`.

### Referência do contacts.xml

O arquivo `contacts.xml` contém elementos XML que controlam a interação do adaptador de sincronização e do aplicativo com o aplicativo de contatos e o Provedor de contatos. Esses elementos são descritos nas seções a seguir.

#### Elemento <ContactsAccountType>

O elemento `<ContactsAccountType>` controla a interação do aplicativo com o aplicativo de contatos. Ele tem a seguinte sintaxe:

```xml
<ContactsAccountType
        xmlns:android="http://schemas.android.com/apk/res/android"
        inviteContactActivity="activity_name"
        inviteContactActionLabel="invite_command_text"
        viewContactNotifyService="view_notify_service"
        viewGroupActivity="group_view_activity"
        viewGroupActionLabel="group_action_text"
        viewStreamItemActivity="viewstream_activity_name"
        viewStreamItemPhotoActivity="viewphotostream_activity_name">
```

**contido em:**

```
res/xml/contacts.xml
```

**pode conter:**

**`<ContactsDataKind>`**

**Descrição:**

Declara componentes e rótulos da IU do Android que permitem aos usuários convidar um dos seus contatos para uma rede social, notificar usuários quando um dos streams de redes sociais é atualizado etc.

O prefixo `android:` do atributo não é necessário para os atributos de `<ContactsAccountType>`.

**Atributos:**

- `inviteContactActivity`

  É o nome de classe totalmente qualificado da atividade no aplicativo que você quer ativar quando o usuário seleciona **Add connection** no aplicativo de contatos do dispositivo.

- `inviteContactActionLabel`

  É uma string de texto exibida para a atividade especificada em `inviteContactActivity`, no menu **Add connection**. Por exemplo, você pode usar a string “Siga-me na rede”. Você pode usar um identificador do recurso da string nesse rótulo.

- `viewContactNotifyService`

  É o nome de classe totalmente qualificado de um serviço no aplicativo que deve receber notificações de quando o usuário exibe um contato. Essa notificação é enviada pelo aplicativo de contatos do dispositivo e permite que o aplicativo adie operações de dados intensivos até que elas sejam necessárias. Por exemplo, o aplicativo pode responder a essa notificação lendo e exibindo a foto de alta resolução do contato e os itens de stream social mais recentes. Esse recurso é descrito com mais detalhes na seção [Interações de streams sociais](https://developer.android.com/guide/topics/providers/contacts-provider#SocialStreamInteraction).

- `viewGroupActivity`

  É o nome de classe totalmente qualificado de uma atividade no aplicativo que pode exibir informações de grupo. Quando o usuário clica no rótulo do grupo no aplicativo de contatos do dispositivo, a IU dessa atividade é exibida.

- `viewGroupActionLabel`

  É o rótulo que o aplicativo de contatos exibe para um controle de IU que permite que o usuário veja grupos no seu aplicativo.Um identificador de recurso de string é permitido para esse atributo.

- `viewStreamItemActivity`

  É o nome de classe totalmente qualificado de uma atividade no aplicativo que o aplicativo de contatos do dispositivo executa quando o usuário clica em um item de stream de um contato bruto.

- `viewStreamItemPhotoActivity`

  É o nome de classe totalmente qualificado de uma atividade no aplicativo que o aplicativo de contatos do dispositivo executa quando o usuário clica em uma foto no item de stream de um contato bruto.

#### Elemento <ContactsDataKind>

O elemento `<ContactsDataKind>` controla a exibição das linhas de dados personalizados do aplicativo na IU do aplicativo de contatos. Ele tem a seguinte sintaxe:

```xml
<ContactsDataKind
        android:mimeType="MIMEtype"
        android:icon="icon_resources"
        android:summaryColumn="column_name"
        android:detailColumn="column_name">
```

**contido em:**

```
<ContactsAccountType>
```

**Descrição:**

Use esse elemento para que o aplicativo de contatos exiba o conteúdo de uma linha de dados personalizados como parte dos detalhes de um contato bruto. Cada elemento filho `<ContactsDataKind>` de `<ContactsAccountType>` representa um tipo de linha de dados personalizados que o adaptador de sincronização adiciona à tabela `ContactsContract.Data`. Adicione um elemento `<ContactsDataKind>` para cada tipo MIME personalizado usado. Não será necessário adicionar o elemento se você tiver uma linha de dados personalizados em que não deseja exibir dados.

**Atributos:**

- `android:mimeType`

  É o tipo MIME personalizado definido para um dos tipos de linha de dados personalizados na tabela `ContactsContract.Data`. Por exemplo, o valor `vnd.android.cursor.item/vnd.example.locationstatus` poderia ser um tipo MIME personalizado para uma linha de dados que registra a última localização conhecida do contato.

- `android:icon`

  É um [recurso drawable](https://developer.android.com/guide/topics/resources/drawable-resource) do Android que o aplicativo de contatos exibe próximo aos dados. Use isso para indicar ao usuário que os dados são provenientes do seu serviço.

- `android:summaryColumn`

  É o nome de coluna do primeiro de dois valores recuperados da linha de dados. O valor é exibido como a primeira linha da entrada para essa linha de dados. A primeira linha se destina ao uso como um resumo dos dados, mas isso é opcional. Veja também [android:detailColumn](https://developer.android.com/guide/topics/providers/contacts-provider#detailColumn).

- `android:detailColumn`

  É o nome de coluna do segundo de dois valores recuperados da linha de dados. O valor é exibido como a segunda linha da entrada para essa linha de dados. Veja também `android:summaryColumn`.

## Recursos adicionais do Provedor de contatos

Além dos principais recursos descritos nas seções anteriores, o Provedor de contatos oferece estes recursos úteis para trabalhar com dados de contatos:

- Grupos de contatos
- Recursos de foto

### Grupos de contatos

O Provedor de contatos pode, opcionalmente, rotular grupos de contatos relacionados com dados de **grupo**. Se o servidor associado a uma conta de usuário desejar manter grupos, o adaptador de sincronização para o tipo da conta deverá transferir dados de grupo entre o Provedor de contatos e o servidor. Quando o usuário adiciona um novo contato ao servidor e, em seguida, coloca esse contato em um novo grupo, o adaptador de sincronização deve adicionar o novo grupo à tabela `ContactsContract.Groups`. O grupo ou grupos a que um contato bruto pertence são armazenados na tabela `ContactsContract.Data`, usando o tipo MIME `ContactsContract.CommonDataKinds.GroupMembership`.

Se estiver projetando um adaptador de sincronização que adicionará dados de contato bruto do servidor ao Provedor de contatos e não estiver usando grupos, será necessário fazer com que o provedor torne os dados visíveis. No código que é executado quando um usuário adiciona uma conta ao dispositivo, atualize a linha `ContactsContract.Settings` que o Provedor de contatos adicionou para a conta. Nessa linha, defina o valor da coluna `Settings.UNGROUPED_VISIBLE` como 1. Ao fazê-lo, o Provedor de contatos sempre deixará visíveis os dados dos contatos, mesmo sem usar grupos.

### Fotos de contatos

A tabela `ContactsContract.Data` armazena fotos como linhas com tipo MIME `Photo.CONTENT_ITEM_TYPE`. A coluna `CONTACT_ID` da linha é vinculada à coluna `_ID` do contato bruto a que pertence. A classe `ContactsContract.Contacts.Photo` define uma subtabela de `ContactsContract.Contacts` contendo informações de foto de uma foto principal do contato, que é a foto principal do contato bruto principal do contato. Da mesma forma, a classe `ContactsContract.RawContacts.DisplayPhoto` define uma subtabela de `ContactsContract.RawContacts` contendo informações de foto de uma foto principal do contato bruto.

A documentação de referência de `ContactsContract.Contacts.Photo` e `ContactsContract.RawContacts.DisplayPhoto` contém exemplos de recuperação de informações de foto. Não há classe conveniente para recuperar a miniatura principal de um contato bruto, mas é possível enviar uma consulta à tabela `ContactsContract.Data` selecionando no `_ID` do contato bruto o `Photo.CONTENT_ITEM_TYPE` e a coluna[IS_PRIMARY](https://developer.android.com/reference/android/provider/ContactsContract.DataColumns#IS_PRIMARY) para encontrar a linha da foto principal do contato bruto.

Dados de streams sociais de uma pessoa também podem conter fotos. Elas são armazenadas na tabela android.provider.ContactsContract.StreamItemPhotos, descrita em mais detalhes na seção [Fotos de streams sociais](https://developer.android.com/guide/topics/providers/contacts-provider#StreamPhotos).

Fonte: https://developer.android.com/guide/topics/providers/contacts-provider