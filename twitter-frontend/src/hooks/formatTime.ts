export function formatRelativeTime(isoString: string): string {
  if (!isoString) return '';
  const date = new Date(isoString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffSec = Math.floor(diffMs / 1000);
  const diffMin = Math.floor(diffSec / 60);
  const diffHour = Math.floor(diffMin / 60);

  if (diffSec < 60) return 'now';
  if (diffMin < 60) return `${diffMin}m`;
  if (diffHour < 24) return `${diffHour}h`;

  const sameYear = date.getFullYear() === now.getFullYear();
  const options: Intl.DateTimeFormatOptions = sameYear
    ? { month: 'short', day: 'numeric' }
    : { month: 'short', day: 'numeric', year: 'numeric' };
  return date.toLocaleDateString('en-US', options);
}

/** Full clock time for message bubbles, e.g. "3:41 PM" */
export function formatClockTime(isoString: string): string {
  if (!isoString) return '';
  return new Date(isoString).toLocaleTimeString('en-US', {
    hour: 'numeric',
    minute: '2-digit',
  });
}

/** Day divider label, e.g. "Today", "Yesterday", "June 12, 2025" */
export function formatDayDivider(isoString: string): string {
  const date = new Date(isoString);
  const now = new Date();
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);

  const isSameDay = (a: Date, b: Date) =>
    a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();

  if (isSameDay(date, now)) return 'Today';
  if (isSameDay(date, yesterday)) return 'Yesterday';

  const sameYear = date.getFullYear() === now.getFullYear();
  return date.toLocaleDateString('en-US', {
    month: 'long',
    day: 'numeric',
    year: sameYear ? undefined : 'numeric',
  });
}

/** Format last seen details, e.g. "Last seen just now", "Last seen 2 minutes ago", "Last seen yesterday at 8:15 PM" */
export function formatLastSeen(isoString: string): string {
  if (!isoString) return 'Last seen recently';
  const date = new Date(isoString);
  const now = new Date();
  
  const diffMs = now.getTime() - date.getTime();
  const diffSec = Math.floor(diffMs / 1000);
  const diffMin = Math.floor(diffSec / 60);
  const diffHour = Math.floor(diffMin / 60);

  if (diffSec < 60) {
    return 'Last seen just now';
  }
  if (diffMin < 60) {
    return `Last seen ${diffMin} ${diffMin === 1 ? 'minute' : 'minutes'} ago`;
  }
  if (diffHour < 24) {
    const isSameDay = date.getFullYear() === now.getFullYear() && 
                      date.getMonth() === now.getMonth() && 
                      date.getDate() === now.getDate();
    if (isSameDay) {
      const timeStr = date.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });
      return `Last seen today at ${timeStr}`;
    }
  }

  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  const isYesterday = date.getFullYear() === yesterday.getFullYear() && 
                      date.getMonth() === yesterday.getMonth() && 
                      date.getDate() === yesterday.getDate();

  const timeStr = date.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });
  if (isYesterday) {
    return `Last seen yesterday at ${timeStr}`;
  }

  const sameYear = date.getFullYear() === now.getFullYear();
  const dateStr = date.toLocaleDateString('en-US', {
    month: 'long',
    day: 'numeric',
    year: sameYear ? undefined : 'numeric'
  });
  return `Last seen on ${dateStr} at ${timeStr}`;
}

