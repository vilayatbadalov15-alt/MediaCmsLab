const $ = id => document.getElementById(id);
const formats = {ARTICLE: 'Статья', VIDEO: 'Видео', PODCAST: 'Подкаст', AUDIO: 'Аудио', IMAGE: 'Изображение'};
const statuses = {PUBLISHED: 'Опубликован', DRAFT: 'Черновик', ARCHIVED: 'Архив'};
const form = $('contentForm');
let account = null;
let current = null;
let page = 0;

async function api(path, method = 'GET', body) {
  const headers = {};
  if (account) headers.Authorization = 'Bearer ' + account.token;
  if (body) headers['Content-Type'] = 'application/json';
  const response = await fetch(path, {method, headers, body: body && JSON.stringify(body)});
  if (!response.ok) {
    const errors = {400: 'Проверьте поля формы.', 401: 'Требуется вход. Проверьте почту и пароль.',
      403: 'Недостаточно прав.', 404: 'Материал недоступен.', 409: 'Эта почта уже зарегистрирована.'};
    throw new Error(errors[response.status] || 'Запрос завершился с ошибкой ' + response.status);
  }
  return response.status === 204 ? null : response.json();
}

function report(error) {
  const dialog = document.querySelector('dialog[open]');
  (dialog?.querySelector('[role="alert"]') || $('notice')).textContent = error.message;
}

function show(id) {
  document.querySelector('dialog[open]')?.close();
  $(id).querySelector('[role="alert"]').textContent = '';
  $(id).showModal();
}

function element(tag, text, className) {
  const el = document.createElement(tag);
  el.textContent = text;
  if (className) el.className = className;
  return el;
}

function titleButton(item, className) {
  const button = element('button', item.title, className);
  button.onclick = () => openItem(item.id).catch(report);
  return button;
}

function metadata(item) {
  const date = new Date(item.publishedAt || item.createdAt).toLocaleDateString('ru-RU');
  return [item.authorEmail, date, statuses[item.status]].join(' · ');
}

async function refreshRank() {
  const top = await api('/api/recommendations/top');
  $('emptyRank').hidden = top.length > 0;
  $('ranking').replaceChildren(...top.map(item => {
    const li = document.createElement('li');
    li.append(titleButton(item, 'rankLink'));
    return li;
  }));
}

async function refresh() {
  $('notice').textContent = '';
  $('account').textContent = account ? 'Выйти' : 'Войти';
  $('create').hidden = !['ADMIN', 'EDITOR'].includes(account?.role);
  const feed = await api('/api/content?page=' + page);
  const rows = feed.content.map(item => {
    const row = element('article', '', 'row');
    row.append(element('span', formats[item.type], 'format'), titleButton(item, 'headline'),
      element('p', metadata(item), 'meta'));
    return row;
  });
  $('feed').replaceChildren(...(rows.length ? rows : [element('p', 'Публикаций пока нет.', 'empty')]));
  $('prev').hidden = feed.first;
  $('next').hidden = feed.last;
  await refreshRank();
}

async function openItem(id) {
  current = await api('/api/content/' + id);
  const item = current;
  $('detailType').textContent = formats[item.type];
  $('detailTitle').textContent = item.title;
  $('detailDescription').textContent = item.description || '';
  $('byline').textContent = metadata(item);
  $('detailBody').textContent = item.body || '';
  $('mediaLink').hidden = !/^https?:\/\//i.test(item.mediaUrl || '');
  if (!$('mediaLink').hidden) $('mediaLink').href = item.mediaUrl;
  $('episodeList').replaceChildren(...item.episodes.map(title => element('li', title)));
  $('viewCount').textContent = '';
  const editable = account && (account.role === 'ADMIN' || (account.role === 'EDITOR' && account.email === item.authorEmail));
  $('edit').hidden = $('remove').hidden = !editable;
  show('entry');
  if (item.status === 'PUBLISHED') {
    const count = await api('/api/content/' + id + '/view', 'POST');
    if (current?.id === id) {
      const stats = ['Просмотры за всё время: ' + count];
      if (item.durationSeconds) stats.push('Длительность: ' + item.durationSeconds + ' с');
      $('viewCount').textContent = stats.join(' · ');
    }
    await refreshRank();
  }
}

function chooseFields() {
  for (const label of form.querySelectorAll('[data-types]')) {
    const visible = label.dataset.types.split(' ').includes(form.elements.type.value);
    label.hidden = !visible;
    label.lastElementChild.required = visible;
    label.lastElementChild.disabled = !visible;
  }
}

function startEdit(item) {
  current = item;
  form.reset();
  const fields = form.elements;
  for (const option of fields.type.querySelectorAll('[data-legacy]')) option.remove();
  if (item) {
    if (['AUDIO', 'IMAGE'].includes(item.type)) {
      const option = new Option(formats[item.type], item.type);
      option.dataset.legacy = 'true';
      fields.type.add(option);
    }
    for (const key of ['type', 'title', 'description', 'status', 'body', 'mediaUrl', 'durationSeconds'])
      fields[key].value = item[key] ?? '';
    fields.episodes.value = item.episodes.join('\n');
  }
  chooseFields();
  $('editorTitle').textContent = item ? 'Редактирование' : 'Новый материал';
  show('editor');
}

$('authForm').onsubmit = async event => {
  event.preventDefault();
  const credentials = Object.fromEntries(new FormData(event.target));
  try {
    const session = await api('/api/auth/' + event.submitter.value, 'POST', credentials);
    account = {...session, email: credentials.email};
    event.target.reset();
    $('auth').close();
    page = 0;
    await refresh();
  } catch (e) { report(e); }
};

form.onsubmit = async event => {
  event.preventDefault();
  const data = Object.fromEntries(new FormData(form));
  if (data.durationSeconds) data.durationSeconds = Number(data.durationSeconds);
  if (data.episodes) data.episodes = data.episodes.split('\n').map(s => s.trim()).filter(Boolean);
  if (current) data.tags = current.tags;
  const button = form.querySelector('.primary');
  button.disabled = true;
  try {
    await api('/api/content' + (current ? '/' + current.id : ''), current ? 'PUT' : 'POST', data);
    $('editor').close();
    page = 0;
    await refresh();
  } catch (e) { report(e); }
  finally { button.disabled = false; }
};

$('account').onclick = () => {
  if (!account) return show('auth');
  account = null;
  page = 0;
  refresh().catch(report);
};
$('create').onclick = () => startEdit(null);
$('edit').onclick = () => startEdit(current);
$('remove').onclick = async () => {
  if (!confirm('Удалить материал «' + current.title + '»?')) return;
  try {
    await api('/api/content/' + current.id, 'DELETE');
    $('entry').close();
    page = 0;
    await refresh();
  } catch (e) { report(e); }
};
form.elements.type.onchange = chooseFields;
$('next').onclick = () => { page++; refresh().catch(report); };
$('prev').onclick = () => { page--; refresh().catch(report); };
document.querySelectorAll('[data-close]').forEach(button => {
  button.onclick = () => button.closest('dialog').close();
});
refresh().catch(report);
