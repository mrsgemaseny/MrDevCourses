import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { GalleryPage } from './GalleryPage';

describe('GalleryPage Component with Folder Browser', () => {
  it('renders gallery header, folders, search bar, and items without hero-preview', () => {
    render(<GalleryPage />);

    expect(screen.getByRole('heading', { level: 1, name: 'Галерея' })).toBeInTheDocument();
    expect(screen.getByText('Медиатека платформы')).toBeInTheDocument();
    expect(screen.getByText('Папки и альбомы')).toBeInTheDocument();

    // Folders
    expect(screen.getByRole('button', { name: /Все материалы/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Папка: Фулл \(Brand & Ryo\)/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Стикерпак MrDev/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /YouTube и Промо/i })).toBeInTheDocument();

    // hero-preview.png is strictly removed as requested
    expect(screen.queryByText('Главная страница и видео-курс Вайбкодинг')).not.toBeInTheDocument();

    // Brand and media items exist
    expect(screen.getByText('Как войти в IT (YouTube 16:9)')).toBeInTheDocument();
    expect(screen.getByText('Как войти в IT (Reels 9:16)')).toBeInTheDocument();
    expect(screen.getByText('Логотип MrDEV Bracket')).toBeInTheDocument();
  }, 15000);

  it('filters gallery items when selecting folders', () => {
    render(<GalleryPage />);

    // Switch to Stickers folder
    fireEvent.click(screen.getByRole('button', { name: /Стикерпак MrDev/i }));
    expect(screen.getByRole('heading', { level: 2, name: 'Стикерпак MrDev' })).toBeInTheDocument();
    expect(screen.getByText(/37 эмоций и стикеров/i)).toBeInTheDocument();

    // Switch to Full Folder
    fireEvent.click(screen.getByRole('button', { name: /Папка: Фулл \(Brand & Ryo\)/i }));
    expect(screen.getByRole('heading', { level: 2, name: 'Папка: Фулл (Brand & Ryo)' })).toBeInTheDocument();
    expect(screen.getAllByText(/111/i).length).toBeGreaterThanOrEqual(1);

    // Switch back to All
    fireEvent.click(screen.getByRole('button', { name: /Все материалы/i }));
    expect(screen.getByRole('heading', { level: 2, name: 'Все материалы' })).toBeInTheDocument();
  }, 15000);

  it('filters gallery items using the search input', () => {
    render(<GalleryPage />);

    const searchInput = screen.getByPlaceholderText('Поиск по файлам...');
    fireEvent.change(searchInput, { target: { value: 'Bracket' } });

    // Matches mrdev-bracket-logo
    expect(screen.getByText('Логотип MrDEV Bracket')).toBeInTheDocument();

    // Clear search
    const clearButton = screen.getByLabelText('Очистить поиск');
    fireEvent.click(clearButton);
    expect(searchInput).toHaveValue('');
  });

  it('opens and closes media lightbox modal on click', () => {
    render(<GalleryPage />);

    // Click on preview button of the first card
    const previewButtons = screen.getAllByRole('button', { name: /Просмотр/i });
    fireEvent.click(previewButtons[0]);

    // Dialog should be open
    const modal = screen.getByRole('dialog');
    expect(modal).toBeInTheDocument();

    // Close dialog via close button
    const closeButton = screen.getByLabelText('Закрыть окно');
    fireEvent.click(closeButton);

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('navigates with keyboard arrows and closes with escape in modal', () => {
    render(<GalleryPage />);

    const previewButtons = screen.getAllByRole('button', { name: /Просмотр/i });
    fireEvent.click(previewButtons[0]);

    expect(screen.getByRole('dialog')).toBeInTheDocument();

    // Navigate right
    fireEvent.keyDown(window, { key: 'ArrowRight' });

    // Close with Escape
    fireEvent.keyDown(window, { key: 'Escape' });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
